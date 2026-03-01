package integration_test

import (
	"context"
	"testing"
	"time"

	"github.com/brianvoe/gofakeit/v6"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	apiv1 "router-manager/internal/gen/proto/api/v1"
	"router-manager/tests/suite"
)

func TestSendCommand_ToRouter_HappyPath(t *testing.T) {
	ctx, st := suite.New(t)

	// Given: в системе зарегистрирован роутер
	routerID := insertRouter(ctx, t, st.DB)

	// When: отправляем команду конкретному роутеру
	_, err := st.Client.SendCommand(ctx, &apiv1.SendCommandRequest{
		RouterId:    strPtr(routerID.String()),
		CommandType: "PING",
	})

	// Then: команда создана со статусом PENDING
	require.NoError(t, err)
	assert.Equal(t, 1, countCommands(ctx, t, st.DB, routerID))
	assert.Equal(t, "PENDING", fetchCommandStatus(ctx, t, st.DB, routerID))
}

func TestSendCommand_Broadcast_CreatesCommandForEveryRouter(t *testing.T) {
	ctx, st := suite.New(t)

	// Given: два роутера в системе
	r1 := insertRouter(ctx, t, st.DB)
	r2 := insertRouter(ctx, t, st.DB)

	// When: отправляем broadcast (без RouterId)
	_, err := st.Client.SendCommand(ctx, &apiv1.SendCommandRequest{
		CommandType: "REBOOT",
	})

	// Then: оба роутера получили команду
	require.NoError(t, err)
	assert.Positive(t, countCommands(ctx, t, st.DB, r1))
	assert.Positive(t, countCommands(ctx, t, st.DB, r2))
}

func TestPoll_UpdatesLastSeenAndMarksSent(t *testing.T) {
	ctx, st := suite.New(t)

	// Given: роутер с ожидающей командой
	routerID := insertRouter(ctx, t, st.DB)
	_, err := st.Client.SendCommand(ctx, &apiv1.SendCommandRequest{
		RouterId:    strPtr(routerID.String()),
		CommandType: "PING",
	})
	require.NoError(t, err)

	// When: роутер вызывает Poll
	before := dbNow(ctx, t, st.DB)
	resp, err := st.Client.Poll(ctx, &apiv1.PollRequest{RouterId: routerID.String()})

	// Then: команда переходит в статус SENT, last_seen_at обновляется
	require.NoError(t, err)
	require.NotEmpty(t, resp.Commands)

	row := fetchCommand(ctx, t, st.DB, resp.Commands[0].CommandId)
	assert.Equal(t, "SENT", row.Status)
	assert.NotNil(t, row.SentAt)

	lastSeen := fetchLastSeen(ctx, t, st.DB, routerID)
	require.NotNil(t, lastSeen)
	assert.False(t, lastSeen.Before(before))
}

func TestPoll_ReturnsAlreadySentCommands(t *testing.T) {
	ctx, st := suite.New(t)

	// Given: роутер уже получил команду через первый Poll
	routerID := insertRouter(ctx, t, st.DB)
	_, err := st.Client.SendCommand(ctx, &apiv1.SendCommandRequest{
		RouterId:    strPtr(routerID.String()),
		CommandType: "PING",
	})
	require.NoError(t, err)

	_, err = st.Client.Poll(ctx, &apiv1.PollRequest{RouterId: routerID.String()})
	require.NoError(t, err)

	// When: роутер снова вызывает Poll (не подтвердив команду)
	resp, err := st.Client.Poll(ctx, &apiv1.PollRequest{RouterId: routerID.String()})

	// Then: SENT-команды возвращаются повторно
	require.NoError(t, err)
	assert.NotEmpty(t, resp.Commands)
}

func TestAck_MarksCommandAckedAndUpdatesLastSeen(t *testing.T) {
	ctx, st := suite.New(t)

	// Given: роутер получил команду через Poll
	routerID := insertRouter(ctx, t, st.DB)
	_, err := st.Client.SendCommand(ctx, &apiv1.SendCommandRequest{
		RouterId:    strPtr(routerID.String()),
		CommandType: "REBOOT",
	})
	require.NoError(t, err)

	pollResp, err := st.Client.Poll(ctx, &apiv1.PollRequest{RouterId: routerID.String()})
	require.NoError(t, err)
	require.NotEmpty(t, pollResp.Commands)

	cmdID := pollResp.Commands[0].CommandId

	// When: роутер подтверждает выполнение команды
	before := dbNow(ctx, t, st.DB)
	_, err = st.Client.Ack(ctx, &apiv1.AckRequest{
		RouterId:  routerID.String(),
		CommandId: cmdID,
	})

	// Then: команда переходит в статус ACKED, last_seen_at обновляется
	require.NoError(t, err)

	row := fetchCommand(ctx, t, st.DB, cmdID)
	assert.Equal(t, "ACKED", row.Status)
	assert.NotNil(t, row.AckedAt)

	lastSeen := fetchLastSeen(ctx, t, st.DB, routerID)
	require.NotNil(t, lastSeen)
	assert.False(t, lastSeen.Before(before))
}

func TestFullLifecycle_PendingSentAcked(t *testing.T) {
	ctx, st := suite.New(t)

	// Given: роутер в системе
	routerID := insertRouter(ctx, t, st.DB)

	// When: отправляем команду
	_, err := st.Client.SendCommand(ctx, &apiv1.SendCommandRequest{
		RouterId:    strPtr(routerID.String()),
		CommandType: "PING",
	})
	require.NoError(t, err)

	// Then: статус PENDING
	assert.Equal(t, "PENDING", fetchCommandStatus(ctx, t, st.DB, routerID))

	// When: роутер забирает команду
	pollResp, err := st.Client.Poll(ctx, &apiv1.PollRequest{RouterId: routerID.String()})
	require.NoError(t, err)
	require.NotEmpty(t, pollResp.Commands)
	cmdID := pollResp.Commands[0].CommandId

	// Then: статус SENT
	assert.Equal(t, "SENT", fetchCommand(ctx, t, st.DB, cmdID).Status)

	// When: роутер подтверждает выполнение
	_, err = st.Client.Ack(ctx, &apiv1.AckRequest{
		RouterId:  routerID.String(),
		CommandId: cmdID,
	})
	require.NoError(t, err)

	// Then: статус ACKED
	assert.Equal(t, "ACKED", fetchCommand(ctx, t, st.DB, cmdID).Status)
}

func insertRouter(ctx context.Context, t *testing.T, db *pgxpool.Pool) uuid.UUID {
	t.Helper()
	id := uuid.New()
	_, err := db.Exec(ctx,
		"INSERT INTO routers (id, serial_number, created_at) VALUES (@id, @serialNumber, now())",
		pgx.NamedArgs{
			"id":           id,
			"serialNumber": gofakeit.Numerify("SN-######"),
		},
	)
	require.NoError(t, err)
	return id
}

type commandRow struct {
	Status  string
	SentAt  *time.Time
	AckedAt *time.Time
}

func fetchCommand(ctx context.Context, t *testing.T, db *pgxpool.Pool, commandID string) commandRow {
	t.Helper()
	var row commandRow
	err := db.QueryRow(ctx,
		"SELECT status, sent_at, acked_at FROM commands WHERE id = @id",
		pgx.NamedArgs{"id": commandID},
	).Scan(&row.Status, &row.SentAt, &row.AckedAt)
	require.NoError(t, err)
	return row
}

func fetchCommandStatus(ctx context.Context, t *testing.T, db *pgxpool.Pool, routerID uuid.UUID) string {
	t.Helper()
	var status string
	err := db.QueryRow(ctx,
		"SELECT status FROM commands WHERE router_id = @routerId",
		pgx.NamedArgs{"routerId": routerID},
	).Scan(&status)
	require.NoError(t, err)
	return status
}

func fetchLastSeen(ctx context.Context, t *testing.T, db *pgxpool.Pool, routerID uuid.UUID) *time.Time {
	t.Helper()
	var ts *time.Time
	err := db.QueryRow(ctx,
		"SELECT last_seen_at FROM routers WHERE id = @id",
		pgx.NamedArgs{"id": routerID},
	).Scan(&ts)
	require.NoError(t, err)
	return ts
}

func countCommands(ctx context.Context, t *testing.T, db *pgxpool.Pool, routerID uuid.UUID) int {
	t.Helper()
	var n int
	err := db.QueryRow(ctx,
		"SELECT count(*) FROM commands WHERE router_id = @routerId",
		pgx.NamedArgs{"routerId": routerID},
	).Scan(&n)
	require.NoError(t, err)
	return n
}

/*
	При локальном запуске тестов, бывают ситуации, когда время БД рассинхронизировано с временем ОС, и получаем ошибку,

поэтому получаем время из бд, для проверки
*/
func dbNow(ctx context.Context, t *testing.T, db *pgxpool.Pool) time.Time {
	t.Helper()
	var ts time.Time
	err := db.QueryRow(ctx, "SELECT now()").Scan(&ts)
	require.NoError(t, err)
	return ts
}

func strPtr(s string) *string { return &s }
