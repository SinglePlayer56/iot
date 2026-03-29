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

func (a *App) MustStart(ctx context.Context) *pgxpool.Pool {
	pool, err := a.Start(ctx)
	if err != nil {
		panic(err)
	}
	return pool
}

func (a *App) Start(ctx context.Context) (*pgxpool.Pool, error) {
	if a.pool != nil {
		return nil, nil
	}

	if a.dbURL == "" {
		return nil, fmt.Errorf("postgres db url is empty")
	}

	dbPool, err := pgxpool.New(ctx, a.dbURL)
	if err != nil {
		return nil, err
	}

	if err := dbPool.Ping(ctx); err != nil {
		dbPool.Close()
		return nil, err
	}

	a.pool = dbPool

	if a.log != nil {
		a.log.Info("postgres pool started")
	}

	return dbPool, nil
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
