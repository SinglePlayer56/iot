package db

import (
	"context"
	"fmt"
	"log/slog"

	"github.com/jackc/pgx/v5/pgxpool"
)

type App struct {
	log   *slog.Logger
	dbURL string
	pool  *pgxpool.Pool
}

func New(log *slog.Logger, dbURL string) *App {
	return &App{
		log:   log,
		dbURL: dbURL,
		pool:  nil,
	}
}

func (a *App) MustStart(ctx context.Context) {
	if err := a.Start(ctx); err != nil {
		panic(err)
	}
}

func (a *App) Start(ctx context.Context) error {
	if a.pool != nil {
		return nil
	}

	if a.dbURL == "" {
		return fmt.Errorf("postgres db url is empty")
	}

	dbPool, err := pgxpool.New(ctx, a.dbURL)
	if err != nil {
		return err
	}

	if err := dbPool.Ping(ctx); err != nil {
		dbPool.Close()
		return err
	}

	a.pool = dbPool

	if a.log != nil {
		a.log.Info("postgres pool started")
	}

	return nil
}

func (a *App) Pool() *pgxpool.Pool {
	return a.pool
}

func (a *App) Stop() {
	if a.pool == nil {
		return
	}

	if a.log != nil {
		a.log.Info("stopping postgres pool")
	}

	a.pool.Close()
	a.pool = nil
}
