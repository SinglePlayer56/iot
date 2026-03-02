package command

import (
	"context"
	"fmt"
	"router-manager/internal/domain/models"
	"time"

	trmpgx "github.com/avito-tech/go-transaction-manager/drivers/pgxv5/v2"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
)

type CommandRepository struct {
	db        trmpgx.Tr
	ctxGetter *trmpgx.CtxGetter
}

func New(db trmpgx.Tr, ctxGetter *trmpgx.CtxGetter) *CommandRepository {
	return &CommandRepository{
		db:        db,
		ctxGetter: ctxGetter,
	}
}

func (r *CommandRepository) conn(ctx context.Context) trmpgx.Tr {
	if r.ctxGetter == nil {
		return r.db
	}

	return r.ctxGetter.DefaultTrOrDB(ctx, r.db)
}

func (r *CommandRepository) SaveCommand(ctx context.Context, command models.Command) (*models.Command, error) {

	query := `
		INSERT INTO commands (
			id,
			router_id,
			command_type,
			payload,
			status,
			sent_at,
			acked_at,
			created_at
		)
		VALUES (
			@id,
			@routerId,
			@commandType,
			@payload,
			@status,
			@sentAt,
			@ackedAt,
			@createdAt
		)
		RETURNING
			id,
			router_id,
			command_type,
			payload,
			status,
			sent_at,
			acked_at,
			created_at
	`

	args := pgx.NamedArgs{
		"id":          command.ID,
		"routerId":    command.RouterID,
		"commandType": command.CommandType,
		"payload":     command.Payload,
		"status":      command.Status,
		"sentAt":      command.SentAt,
		"ackedAt":     command.AckedAt,
		"createdAt":   command.CreatedAt,
	}

	var saved models.Command

	err := r.conn(ctx).
		QueryRow(ctx, query, args).
		Scan(
			&saved.ID,
			&saved.RouterID,
			&saved.CommandType,
			&saved.Payload,
			&saved.Status,
			&saved.SentAt,
			&saved.AckedAt,
			&saved.CreatedAt,
		)
	if err != nil {
		return nil, err
	}

	return &saved, nil
}

func (r *CommandRepository) GetCommands(ctx context.Context, routerId uuid.UUID) ([]models.Command, error) {
	query := `
		SELECT
			id,
			router_id,
			command_type,
			payload,
			status,
			sent_at,
			acked_at,
			created_at
		FROM commands
		WHERE router_id = @routerId AND status IN (@pendingStatus, @sentStatus)
	`

	args := pgx.NamedArgs{
		"routerId":      routerId,
		"pendingStatus": models.PENDING,
		"sentStatus":    models.SENT,
	}

	rows, err := r.conn(ctx).Query(ctx, query, args)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	commands := make([]models.Command, 0)
	for rows.Next() {
		var cmd models.Command
		if err := rows.Scan(
			&cmd.ID,
			&cmd.RouterID,
			&cmd.CommandType,
			&cmd.Payload,
			&cmd.Status,
			&cmd.SentAt,
			&cmd.AckedAt,
			&cmd.CreatedAt,
		); err != nil {
			return nil, err
		}

		commands = append(commands, cmd)
	}

	if err := rows.Err(); err != nil {
		return nil, err
	}

	return commands, nil
}

func (r *CommandRepository) SetSentCommandsBatch(ctx context.Context, routerId uuid.UUID, commandIds []uuid.UUID) error {
	if len(commandIds) == 0 {
		return nil
	}

	query := `
		UPDATE commands
		SET sent_at = NOW(), status = @status
		WHERE
			router_id = @routerId
			AND id = ANY(@commandIds::uuid[])
			AND status = @pendingStatus
	`

	args := pgx.NamedArgs{
		"routerId":      routerId,
		"commandIds":    commandIds,
		"status":        models.SENT,
		"pendingStatus": models.PENDING,
	}

	if _, err := r.conn(ctx).Exec(ctx, query, args); err != nil {
		return err
	}

	return nil
}

func (r *CommandRepository) AckCommand(ctx context.Context, routerId uuid.UUID, commandId uuid.UUID) error {
	query := `
		UPDATE commands
		SET acked_at = NOW(), status = @status
		WHERE
			id = @id
			AND router_id = @routerId
			AND status = @currentStatus
	`
	args := pgx.NamedArgs{
		"id":            commandId,
		"routerId":      routerId,
		"status":        models.ACKED,
		"currentStatus": models.SENT,
	}
	tag, err := r.conn(ctx).Exec(ctx, query, args)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return fmt.Errorf(
			"command with id %s for router %s not found or not in %s status",
			commandId,
			routerId,
			models.SENT,
		)
	}
	return nil
}

func (r *CommandRepository) SaveCommandsBatch(ctx context.Context, commands []models.Command) error {
	if len(commands) == 0 {
		return nil
	}

	table := pgx.Identifier{"commands"}
	cols := []string{
		"id",
		"router_id",
		"command_type",
		"payload",
		"status",
		"sent_at",
		"acked_at",
		"created_at",
	}

	src := pgx.CopyFromSlice(len(commands), func(i int) ([]any, error) {
		c := commands[i]
		return []any{
			c.ID,
			c.RouterID,
			c.CommandType,
			c.Payload,
			c.Status,
			c.SentAt,
			c.AckedAt,
			c.CreatedAt,
		}, nil
	})

	_, err := r.conn(ctx).CopyFrom(ctx, table, cols, src)
	if err != nil {
		return err
	}

	return nil
}

func (r *CommandRepository) ExpireSentCommandsBefore(ctx context.Context, threshold time.Time) (int, error) {
	query := `
		UPDATE commands
		SET status = @failedStatus
		WHERE
			status = @sentStatus
			AND sent_at IS NOT NULL
			AND sent_at < @threshold
	`

	args := pgx.NamedArgs{
		"failedStatus": models.FAILED,
		"sentStatus":   models.SENT,
		"threshold":    threshold,
	}

	tag, err := r.conn(ctx).Exec(ctx, query, args)
	if err != nil {
		return 0, err
	}

	return int(tag.RowsAffected()), nil
}
