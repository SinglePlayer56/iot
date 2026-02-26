package app

import (
	"context"
	"log/slog"
	"time"

	trmpgx "github.com/avito-tech/go-transaction-manager/drivers/pgxv5/v2"
	trmmanager "github.com/avito-tech/go-transaction-manager/trm/v2/manager"

	commandexpirationworker "router-manager/internal/app/commandexpiration"
	dbapp "router-manager/internal/app/db"
	grpcapp "router-manager/internal/app/grpc"
	commandrepository "router-manager/internal/repository/command"
	routerrepository "router-manager/internal/repository/router"
	commandservice "router-manager/internal/services/command"
	routerservice "router-manager/internal/services/router"
)

type App struct {
	GRPCServer              *grpcapp.App
	CommandExpirationWorker *commandexpirationworker.App
	DB                      *dbapp.App
}

func New(
	ctx context.Context,
	log *slog.Logger,
	grpcPort int,
	dbUrl string,
	commandTTL time.Duration,
) *App {
	db := dbapp.New(log, dbUrl)
	dbPool := db.MustStart(ctx)

	ctxGetter := trmpgx.DefaultCtxGetter

	commandRepo := commandrepository.New(dbPool, ctxGetter)
	commandService := commandservice.New(log, commandRepo)

	routerRepo := routerrepository.New(dbPool, ctxGetter)
	routerService := routerservice.New(log, routerRepo)

	transactionManager := trmmanager.Must(trmpgx.NewDefaultFactory(dbPool))

	grpcApp := grpcapp.New(log, grpcPort, transactionManager, commandService, routerService)
	commandExpirationWorker := commandexpirationworker.New(ctx, log, transactionManager, commandService, commandTTL)

	return &App{
		GRPCServer:              grpcApp,
		CommandExpirationWorker: commandExpirationWorker,
		DB:                      db,
	}
}
