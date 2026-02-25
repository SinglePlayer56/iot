package app

import (
	"log/slog"

	dbapp "router-manager/internal/app/db"
	grpcapp "router-manager/internal/app/grpc"
)

type App struct {
	GRPCServer *grpcapp.App
	DB         *dbapp.App
}

func New(
	log *slog.Logger,
	grpcPort int,
	dbUrl string,
) *App {
	grpcApp := grpcapp.New(log, grpcPort)
	db := dbapp.New(log, dbUrl)

	return &App{
		GRPCServer: grpcApp,
		DB:         db,
	}
}
