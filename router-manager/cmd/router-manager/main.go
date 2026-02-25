package main

import (
	"log/slog"
	"os"
	"router-manager/internal/app"
	"router-manager/internal/config"
)

func main() {
	cfg := config.MustLoad()
	log := initLogger()

	log.Info("Starting application")

	application := app.New(log, cfg.GRPC.Port)

	application.GRPCServer.MustRun()
}

func initLogger() *slog.Logger {
	return slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))
}
