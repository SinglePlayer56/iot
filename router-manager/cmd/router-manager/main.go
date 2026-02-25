package main

import (
	"context"
	"log/slog"
	"os"
	"os/signal"
	"router-manager/internal/app"
	"router-manager/internal/config"
	"router-manager/internal/lib/migrator"
	"syscall"
)

func main() {
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	cfg := config.MustLoad()
	log := initLogger()

	log.Info("Starting application")

	if err := migrator.Up(ctx, cfg.MigrationPath, cfg.Database.Url); err != nil {
		log.Error("Failed to apply migrations", slog.String("error", err.Error()))
		os.Exit(1)
	}

	log.Info("Migrations applied")

	application := app.New(log, cfg.GRPC.Port, cfg.Database.Url)

	go application.GRPCServer.MustRun()

	<-ctx.Done()
	log.Info("Stopping application", slog.String("reason", "shutdown signal"))

	application.GRPCServer.Stop()
	application.DB.Stop()

	log.Info("Application stopped")
}

func initLogger() *slog.Logger {
	return slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))
}
