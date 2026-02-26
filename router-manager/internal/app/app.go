package app

import (
	"context"
	"log/slog"

	trmpgx "github.com/avito-tech/go-transaction-manager/drivers/pgxv5/v2"
	trmmanager "github.com/avito-tech/go-transaction-manager/trm/v2/manager"

	dbapp "router-manager/internal/app/db"
	grpcapp "router-manager/internal/app/grpc"
	commandrepository "router-manager/internal/repository/command"
	routerrepository "router-manager/internal/repository/router"
	commandservice "router-manager/internal/services/command"
	routerservice "router-manager/internal/services/router"
)

type App struct {
	GRPCServer *grpcapp.App
	DB         *dbapp.App
}

func New(
	ctx context.Context,
	log *slog.Logger,
	grpcPort int,
	dbUrl string,
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

	return &App{
		GRPCServer: grpcApp,
		DB:         db,
	}
}
