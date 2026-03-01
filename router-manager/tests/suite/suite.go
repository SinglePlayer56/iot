package suite

import (
	"context"
	"fmt"
	"log/slog"
	"net"
	"path/filepath"
	"runtime"
	"sync"
	"testing"
	"time"

	trmpgx "github.com/avito-tech/go-transaction-manager/drivers/pgxv5/v2"
	trmmanager "github.com/avito-tech/go-transaction-manager/trm/v2/manager"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/testcontainers/testcontainers-go"
	tcpostgres "github.com/testcontainers/testcontainers-go/modules/postgres"
	"github.com/testcontainers/testcontainers-go/wait"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"

	apiv1 "router-manager/internal/gen/proto/api/v1"
	routermanagergrpc "router-manager/internal/grpc"
	"router-manager/internal/lib/migrator"
	commandrepository "router-manager/internal/repository/command"
	routerrepository "router-manager/internal/repository/router"
	commandservice "router-manager/internal/services/command"
	routerservice "router-manager/internal/services/router"
)

type Suite struct {
	Client apiv1.RouterManagerClient
	DB     *pgxpool.Pool
}

var (
	once     sync.Once
	instance *Suite
	initErr  error
)

func New(t *testing.T) (context.Context, *Suite) {
	t.Helper()
	once.Do(func() {
		instance, initErr = newSuite()
	})
	if initErr != nil {
		t.Fatalf("suite init: %v", initErr)
	}
	return context.Background(), instance
}

func newSuite() (*Suite, error) {
	ctx := context.Background()

	pgContainer, err := tcpostgres.Run(ctx,
		"postgres:17-alpine",
		tcpostgres.WithDatabase("router-manager"),
		tcpostgres.WithUsername("postgres"),
		tcpostgres.WithPassword("postgres"),
		testcontainers.WithWaitStrategy(
			wait.ForLog("database system is ready to accept connections").
				WithOccurrence(2).
				WithStartupTimeout(60*time.Second),
		),
	)
	if err != nil {
		return nil, fmt.Errorf("postgres.Run: %w", err)
	}

	connStr, err := pgContainer.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		return nil, fmt.Errorf("ConnectionString: %w", err)
	}

	if err := migrator.Up(ctx, migrationsDir(), connStr); err != nil {
		return nil, fmt.Errorf("migrations: %w", err)
	}

	pool, err := pgxpool.New(ctx, connStr)
	if err != nil {
		return nil, fmt.Errorf("pgxpool.New: %w", err)
	}

	client, err := newGRPCClient(pool)
	if err != nil {
		return nil, fmt.Errorf("grpc client: %w", err)
	}

	return &Suite{
		Client: client,
		DB:     pool,
	}, nil
}

func newGRPCClient(pool *pgxpool.Pool) (apiv1.RouterManagerClient, error) {
	ctxGetter := trmpgx.DefaultCtxGetter

	cmdRepo := commandrepository.New(pool, ctxGetter)
	cmdSvc := commandservice.New(slog.Default(), cmdRepo)

	rtrRepo := routerrepository.New(pool, ctxGetter)
	rtrSvc := routerservice.New(slog.Default(), rtrRepo)

	txMgr := trmmanager.Must(trmpgx.NewDefaultFactory(pool))

	srv := grpc.NewServer()
	routermanagergrpc.Register(srv, txMgr, cmdSvc, rtrSvc)

	lis, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		return nil, fmt.Errorf("net.Listen: %w", err)
	}
	go func() { _ = srv.Serve(lis) }()

	conn, err := grpc.NewClient(
		lis.Addr().String(),
		grpc.WithTransportCredentials(insecure.NewCredentials()),
	)
	if err != nil {
		return nil, fmt.Errorf("grpc.NewClient: %w", err)
	}

	return apiv1.NewRouterManagerClient(conn), nil
}

func migrationsDir() string {
	_, filename, _, _ := runtime.Caller(0)
	return filepath.Join(filepath.Dir(filename), "..", "..", "migrations")
}
