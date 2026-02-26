package app

import (
	"context"
	"log/slog"

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

	commandRepo := commandrepository.New(dbPool)
	commandService := commandservice.New(log, commandRepo)

	routerRepo := routerrepository.New(dbPool)
	routerService := routerservice.New(log, routerRepo)

	grpcApp := grpcapp.New(log, grpcPort, commandService, routerService)

	return &App{
		GRPCServer: grpcApp,
		DB:         db,
	}
}
