package unit

import (
	"context"
	"errors"
	"io"
	"log/slog"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"router-manager/internal/domain/models"
	commandservice "router-manager/internal/services/command"
)

type mockCommandRepository struct {
	saveCommandFn            func(ctx context.Context, command models.Command) (*models.Command, error)
	getCommandsFn            func(ctx context.Context, routerId uuid.UUID) ([]models.Command, error)
	setSentCommandsBatchFn   func(ctx context.Context, routerId uuid.UUID, commandIds []uuid.UUID) error
	ackCommandFn             func(ctx context.Context, routerId uuid.UUID, commandId uuid.UUID) error
	saveCommandsBatchFn      func(ctx context.Context, commands []models.Command) error
	expireSentCommandsBefore func(ctx context.Context, threshold time.Time) (int, error)
}

func (m *mockCommandRepository) SaveCommand(ctx context.Context, command models.Command) (*models.Command, error) {
	if m.saveCommandFn != nil {
		return m.saveCommandFn(ctx, command)
	}
	return &command, nil
}

func (m *mockCommandRepository) GetCommands(ctx context.Context, routerId uuid.UUID) ([]models.Command, error) {
	if m.getCommandsFn != nil {
		return m.getCommandsFn(ctx, routerId)
	}
	return nil, nil
}

func (m *mockCommandRepository) SetSentCommandsBatch(ctx context.Context, routerId uuid.UUID, commandIds []uuid.UUID) error {
	if m.setSentCommandsBatchFn != nil {
		return m.setSentCommandsBatchFn(ctx, routerId, commandIds)
	}
	return nil
}

func (m *mockCommandRepository) AckCommand(ctx context.Context, routerId uuid.UUID, commandId uuid.UUID) error {
	if m.ackCommandFn != nil {
		return m.ackCommandFn(ctx, routerId, commandId)
	}
	return nil
}

func (m *mockCommandRepository) SaveCommandsBatch(ctx context.Context, commands []models.Command) error {
	if m.saveCommandsBatchFn != nil {
		return m.saveCommandsBatchFn(ctx, commands)
	}
	return nil
}

func (m *mockCommandRepository) ExpireSentCommandsBefore(ctx context.Context, threshold time.Time) (int, error) {
	if m.expireSentCommandsBefore != nil {
		return m.expireSentCommandsBefore(ctx, threshold)
	}
	return 0, nil
}

func testLogger() *slog.Logger {
	return slog.New(slog.NewTextHandler(io.Discard, nil))
}

func TestSaveCommand_AssignsUUIDAndSetsPending(t *testing.T) {
	routerID := uuid.New()

	// Given: команда без ID
	svc := commandservice.New(testLogger(), &mockCommandRepository{})
	cmd := models.Command{ID: uuid.Nil, RouterID: &routerID, CommandType: models.PING}

	// When
	result, err := svc.SaveCommand(context.Background(), cmd)

	// Then: назначается новый UUID, статус PENDING
	require.NoError(t, err)
	assert.NotEqual(t, uuid.Nil, result.ID)
	assert.Equal(t, models.PENDING, result.Status)
}

func TestSaveCommand_KeepsExistingUUID(t *testing.T) {
	routerID := uuid.New()
	existingID := uuid.New()

	// Given: команда с уже заданным ID
	svc := commandservice.New(testLogger(), &mockCommandRepository{})
	cmd := models.Command{ID: existingID, RouterID: &routerID, CommandType: models.REBOOT}

	// When
	result, err := svc.SaveCommand(context.Background(), cmd)

	// Then: ID не изменяется, статус PENDING
	require.NoError(t, err)
	assert.Equal(t, existingID, result.ID)
	assert.Equal(t, models.PENDING, result.Status)
}

func TestSaveCommand_WhenCommandTypeEmpty_ReturnsError(t *testing.T) {
	routerID := uuid.New()

	// Given: команда без типа
	svc := commandservice.New(testLogger(), &mockCommandRepository{})
	cmd := models.Command{ID: uuid.New(), RouterID: &routerID, CommandType: ""}

	// When
	result, err := svc.SaveCommand(context.Background(), cmd)

	// Then
	require.Error(t, err)
	assert.Nil(t, result)
}

func TestSaveCommand_WhenRepoFails_ReturnsError(t *testing.T) {
	routerID := uuid.New()

	// Given: репозиторий возвращает ошибку
	repo := &mockCommandRepository{
		saveCommandFn: func(_ context.Context, _ models.Command) (*models.Command, error) {
			return nil, errors.New("db error")
		},
	}
	svc := commandservice.New(testLogger(), repo)
	cmd := models.Command{ID: uuid.New(), RouterID: &routerID, CommandType: models.PING}

	// When
	result, err := svc.SaveCommand(context.Background(), cmd)

	// Then
	require.Error(t, err)
	assert.Nil(t, result)
}

func TestGetCommands_ReturnsCommandsList(t *testing.T) {
	routerID := uuid.New()
	expected := []models.Command{
		{ID: uuid.New(), CommandType: models.PING, Status: models.PENDING},
		{ID: uuid.New(), CommandType: models.REBOOT, Status: models.SENT},
	}

	// Given: репозиторий возвращает список команд
	repo := &mockCommandRepository{
		getCommandsFn: func(_ context.Context, _ uuid.UUID) ([]models.Command, error) {
			return expected, nil
		},
	}
	svc := commandservice.New(testLogger(), repo)

	// When
	result, err := svc.GetCommands(context.Background(), routerID)

	// Then
	require.NoError(t, err)
	assert.Len(t, result, 2)
}

func TestGetCommands_WhenRouterIDNil_ReturnsError(t *testing.T) {
	// Given: невалидный router ID
	svc := commandservice.New(testLogger(), &mockCommandRepository{})

	// When
	result, err := svc.GetCommands(context.Background(), uuid.Nil)

	// Then
	require.Error(t, err)
	assert.Nil(t, result)
}

func TestGetCommands_WhenRepoFails_ReturnsError(t *testing.T) {
	routerID := uuid.New()

	// Given: репозиторий возвращает ошибку
	repo := &mockCommandRepository{
		getCommandsFn: func(_ context.Context, _ uuid.UUID) ([]models.Command, error) {
			return nil, errors.New("db error")
		},
	}
	svc := commandservice.New(testLogger(), repo)

	// When
	result, err := svc.GetCommands(context.Background(), routerID)

	// Then
	require.Error(t, err)
	assert.Nil(t, result)
}

func TestSetSendCommandsBatch_HappyPath(t *testing.T) {
	routerID, id1, id2 := uuid.New(), uuid.New(), uuid.New()

	// Given: валидные данные
	svc := commandservice.New(testLogger(), &mockCommandRepository{})

	// When
	err := svc.SetSendCommandsBatch(context.Background(), routerID, []uuid.UUID{id1, id2})

	// Then
	require.NoError(t, err)
}

func TestSetSendCommandsBatch_WhenRouterIDNil_ReturnsError(t *testing.T) {
	// Given: невалидный router ID
	svc := commandservice.New(testLogger(), &mockCommandRepository{})

	// When
	err := svc.SetSendCommandsBatch(context.Background(), uuid.Nil, []uuid.UUID{uuid.New()})

	// Then
	require.Error(t, err)
}

func TestSetSendCommandsBatch_WhenCommandIDNil_ReturnsError(t *testing.T) {
	routerID := uuid.New()

	// Given: один из command ID равен nil
	svc := commandservice.New(testLogger(), &mockCommandRepository{})

	// When
	err := svc.SetSendCommandsBatch(context.Background(), routerID, []uuid.UUID{uuid.New(), uuid.Nil})

	// Then
	require.Error(t, err)
}

func TestSetSendCommandsBatch_WhenRepoFails_ReturnsError(t *testing.T) {
	routerID := uuid.New()

	// Given: репозиторий возвращает ошибку
	repo := &mockCommandRepository{
		setSentCommandsBatchFn: func(_ context.Context, _ uuid.UUID, _ []uuid.UUID) error {
			return errors.New("db error")
		},
	}
	svc := commandservice.New(testLogger(), repo)

	// When
	err := svc.SetSendCommandsBatch(context.Background(), routerID, []uuid.UUID{uuid.New()})

	// Then
	require.Error(t, err)
}

func TestSetAckCommand_HappyPath(t *testing.T) {
	routerID, commandID := uuid.New(), uuid.New()

	// Given: валидные данные
	svc := commandservice.New(testLogger(), &mockCommandRepository{})

	// When
	err := svc.SetAckCommand(context.Background(), routerID, commandID)

	// Then
	require.NoError(t, err)
}

func TestSetAckCommand_WhenRouterIDNil_ReturnsError(t *testing.T) {
	// Given: невалидный router ID
	svc := commandservice.New(testLogger(), &mockCommandRepository{})

	// When
	err := svc.SetAckCommand(context.Background(), uuid.Nil, uuid.New())

	// Then
	require.Error(t, err)
}

func TestSetAckCommand_WhenCommandIDNil_ReturnsError(t *testing.T) {
	// Given: невалидный command ID
	svc := commandservice.New(testLogger(), &mockCommandRepository{})

	// When
	err := svc.SetAckCommand(context.Background(), uuid.New(), uuid.Nil)

	// Then
	require.Error(t, err)
}

func TestSetAckCommand_WhenRepoFails_ReturnsError(t *testing.T) {
	routerID, commandID := uuid.New(), uuid.New()

	// Given: репозиторий возвращает ошибку
	repo := &mockCommandRepository{
		ackCommandFn: func(_ context.Context, _ uuid.UUID, _ uuid.UUID) error {
			return errors.New("db error")
		},
	}
	svc := commandservice.New(testLogger(), repo)

	// When
	err := svc.SetAckCommand(context.Background(), routerID, commandID)

	// Then
	require.Error(t, err)
}

// ── SaveCommandsBatch ─────────────────────────────────────────────────────────

func TestSaveCommandsBatch_HappyPath(t *testing.T) {
	commands := []models.Command{
		{ID: uuid.New(), CommandType: models.PING},
		{ID: uuid.New(), CommandType: models.REBOOT},
	}

	// Given: список команд
	svc := commandservice.New(testLogger(), &mockCommandRepository{})

	// When
	err := svc.SaveCommandsBatch(context.Background(), commands)

	// Then
	require.NoError(t, err)
}

func TestSaveCommandsBatch_WithEmptySlice_NoError(t *testing.T) {
	// Given: пустой список команд
	svc := commandservice.New(testLogger(), &mockCommandRepository{})

	// When
	err := svc.SaveCommandsBatch(context.Background(), []models.Command{})

	// Then
	require.NoError(t, err)
}

func TestSaveCommandsBatch_WhenRepoFails_ReturnsError(t *testing.T) {
	// Given: репозиторий возвращает ошибку
	repo := &mockCommandRepository{
		saveCommandsBatchFn: func(_ context.Context, _ []models.Command) error {
			return errors.New("db error")
		},
	}
	svc := commandservice.New(testLogger(), repo)

	// When
	err := svc.SaveCommandsBatch(context.Background(), []models.Command{{ID: uuid.New(), CommandType: models.PING}})

	// Then
	require.Error(t, err)
}

// ── ExpireOldCommands ─────────────────────────────────────────────────────────

func TestExpireOldCommands_ReturnsExpiredCount(t *testing.T) {
	// Given: репозиторий сообщает, что истекли 3 команды
	repo := &mockCommandRepository{
		expireSentCommandsBefore: func(_ context.Context, _ time.Time) (int, error) {
			return 3, nil
		},
	}
	svc := commandservice.New(testLogger(), repo)

	// When
	count, err := svc.ExpireOldCommands(context.Background(), 5*time.Minute)

	// Then
	require.NoError(t, err)
	assert.Equal(t, 3, count)
}

func TestExpireOldCommands_WhenTTLZero_ReturnsError(t *testing.T) {
	// Given: невалидный TTL
	svc := commandservice.New(testLogger(), &mockCommandRepository{})

	// When
	count, err := svc.ExpireOldCommands(context.Background(), 0)

	// Then
	require.Error(t, err)
	assert.Zero(t, count)
}

func TestExpireOldCommands_WhenTTLNegative_ReturnsError(t *testing.T) {
	// Given: отрицательный TTL
	svc := commandservice.New(testLogger(), &mockCommandRepository{})

	// When
	count, err := svc.ExpireOldCommands(context.Background(), -time.Minute)

	// Then
	require.Error(t, err)
	assert.Zero(t, count)
}

func TestExpireOldCommands_WhenRepoFails_ReturnsError(t *testing.T) {
	// Given: репозиторий возвращает ошибку
	repo := &mockCommandRepository{
		expireSentCommandsBefore: func(_ context.Context, _ time.Time) (int, error) {
			return 0, errors.New("db error")
		},
	}
	svc := commandservice.New(testLogger(), repo)

	// When
	count, err := svc.ExpireOldCommands(context.Background(), 10*time.Minute)

	// Then
	require.Error(t, err)
	assert.Zero(t, count)
}
