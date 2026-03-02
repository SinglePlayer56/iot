package router

import (
	"context"
	"fmt"

	trmpgx "github.com/avito-tech/go-transaction-manager/drivers/pgxv5/v2"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
)

type RouterRepository struct {
	db        trmpgx.Tr
	ctxGetter *trmpgx.CtxGetter
}

func New(db trmpgx.Tr, ctxGetter *trmpgx.CtxGetter) *RouterRepository {
	return &RouterRepository{
		db:        db,
		ctxGetter: ctxGetter,
	}
}

func (r *RouterRepository) conn(ctx context.Context) trmpgx.Tr {
	if r.ctxGetter == nil {
		return r.db
	}

	return r.ctxGetter.DefaultTrOrDB(ctx, r.db)
}

func (r *RouterRepository) GetAllRoutersId(ctx context.Context) ([]uuid.UUID, error) {
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

func (r *RouterRepository) UpdateLastSeen(ctx context.Context, id uuid.UUID) error {
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
