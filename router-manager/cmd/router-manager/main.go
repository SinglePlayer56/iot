package main

import (
	"log/slog"
	"os"
	"os/signal"
	"router-manager/internal/app"
	"router-manager/internal/config"
	"syscall"
)

func main() {
	cfg := config.MustLoad()
	log := initLogger()

	log.Info("Starting application")

	application := app.New(log, cfg.GRPC.Port)

	go application.GRPCServer.MustRun()

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)

	sign := <-stop

	log.Info("Stopping application", slog.String("signal", sign.String()))

	application.GRPCServer.Stop()

	log.Info("Application stopped")
}

func initLogger() *slog.Logger {
	return slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))
}
