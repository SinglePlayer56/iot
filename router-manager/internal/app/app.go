package app

import (
	"log/slog"

	grpcapp "router-manager/internal/app/grpc"
)

type App struct {
	GRPCServer *grpcapp.App
}

func New(
	log *slog.Logger,
	grpcPort int,
) *App {
	grpcApp := grpcapp.New(log, grpcPort)

	return &App{
		GRPCServer: grpcApp,
	}
}
