package router

import (
	"context"
	"errors"
	"fmt"
	"router-manager/internal/domain/models"

	trmpgx "github.com/avito-tech/go-transaction-manager/drivers/pgxv5/v2"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
)

type Repository struct {
	db        trmpgx.Tr
	ctxGetter *trmpgx.CtxGetter
}

func New(db trmpgx.Tr, ctxGetter *trmpgx.CtxGetter) *Repository {
	return &Repository{
		db:        db,
		ctxGetter: ctxGetter,
	}
}

func (r *Repository) conn(ctx context.Context) trmpgx.Tr {
	if r.ctxGetter == nil {
		return r.db
	}

	return r.ctxGetter.DefaultTrOrDB(ctx, r.db)
}

func (r *Repository) GetAllRoutersId(ctx context.Context) ([]uuid.UUID, error) {
	query := `SELECT id FROM routers`
	rows, err := r.conn(ctx).Query(ctx, query)
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
	query := `SELECT id, serial_number, last_seen_at, created_at FROM routers WHERE id = @id`
	args := pgx.NamedArgs{
		"id": id,
	}

	var router models.Router

	err := r.conn(ctx).QueryRow(ctx, query, args).
		Scan(
			&router.ID,
			&router.SerialNumber,
			&router.LastSeenAt,
			&router.CreatedAt,
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

	tag, err := r.conn(ctx).Exec(ctx, query, args)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return fmt.Errorf("router with id %s not found", id)
	}
	return nil
}
