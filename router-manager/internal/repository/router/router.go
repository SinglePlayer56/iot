package router

import (
	"context"
	"errors"
	"fmt"
	"router-manager/internal/domain/models"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
)

type Storage interface {
	QueryRow(ctx context.Context, sql string, args ...any) pgx.Row
	Query(ctx context.Context, sql string, args ...any) (pgx.Rows, error)
	Exec(ctx context.Context, sql string, args ...interface{}) (pgconn.CommandTag, error)
}

type Repository struct {
	storage Storage
}

func New(storage Storage) *Repository {
	return &Repository{
		storage: storage,
	}
}

func (r *Repository) GetAllRoutersId(ctx context.Context) ([]uuid.UUID, error) {
	query := `SELECT id FROM routers`
	rows, err := r.storage.Query(ctx, query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	routersId := make([]uuid.UUID, 0)
	for rows.Next() {
		var routerId uuid.UUID
		if err := rows.Scan(&routerId); err != nil {
			return nil, err
		}

		routersId = append(routersId, routerId)
	}

	if err := rows.Err(); err != nil {
		return nil, err
	}

	return routersId, nil
}

func (r *Repository) FindById(ctx context.Context, id uuid.UUID) (*models.Router, error) {
	query := `SELECT id, serial_number, last_seen_at FROM routers WHERE id = @id`
	args := pgx.NamedArgs{
		"id": id,
	}

	var router models.Router

	err := r.storage.QueryRow(ctx, query, args).
		Scan(
			&router.ID,
			&router.SerialNumber,
			&router.LastSeenAt,
		)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, fmt.Errorf("router with id %s not found", id)
		}
		return nil, err
	}

	return &router, nil
}

func (r *Repository) UpdateLastSeen(ctx context.Context, id uuid.UUID) error {
	query := `UPDATE routers SET last_seen_at = now() WHERE id = @id`
	args := pgx.NamedArgs{
		"id": id,
	}

	tag, err := r.storage.Exec(ctx, query, args)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return fmt.Errorf("router with id %s not found", id)
	}
	return nil
}
