package unit

import (
	"context"
	"errors"
	"testing"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	routerservice "router-manager/internal/services/router"
)

type mockRouterRepository struct {
	getAllRoutersIdFn func(ctx context.Context) ([]uuid.UUID, error)
	updateLastSeenFn  func(ctx context.Context, id uuid.UUID) error
}

func (m *mockRouterRepository) GetAllRoutersId(ctx context.Context) ([]uuid.UUID, error) {
	if m.getAllRoutersIdFn != nil {
		return m.getAllRoutersIdFn(ctx)
	}
	return nil, nil
}

func (m *mockRouterRepository) UpdateLastSeen(ctx context.Context, id uuid.UUID) error {
	if m.updateLastSeenFn != nil {
		return m.updateLastSeenFn(ctx, id)
	}
	return nil
}

func TestGetAllRoutersId_ReturnsIDsList(t *testing.T) {
	ids := []uuid.UUID{uuid.New(), uuid.New(), uuid.New()}

	// Given: репозиторий возвращает список роутеров
	repo := &mockRouterRepository{
		getAllRoutersIdFn: func(_ context.Context) ([]uuid.UUID, error) {
			return ids, nil
		},
	}
	svc := routerservice.New(testLogger(), repo)

	// When
	result, err := svc.GetAllRoutersId(context.Background())

	// Then
	require.NoError(t, err)
	assert.Len(t, result, 3)
}

func TestGetAllRoutersId_WhenRepoFails_ReturnsError(t *testing.T) {
	// Given: репозиторий возвращает ошибку
	repo := &mockRouterRepository{
		getAllRoutersIdFn: func(_ context.Context) ([]uuid.UUID, error) {
			return nil, errors.New("db error")
		},
	}
	svc := routerservice.New(testLogger(), repo)

	// When
	result, err := svc.GetAllRoutersId(context.Background())

	// Then
	require.Error(t, err)
	assert.Nil(t, result)
}

func TestUpdateLastSeen_HappyPath(t *testing.T) {
	routerID := uuid.New()

	// Given: валидный роутер
	svc := routerservice.New(testLogger(), &mockRouterRepository{})

	// When
	err := svc.UpdateLastSeen(context.Background(), routerID)

	// Then
	require.NoError(t, err)
}

func TestUpdateLastSeen_WhenRepoFails_ReturnsError(t *testing.T) {
	routerID := uuid.New()

	// Given: репозиторий возвращает ошибку
	repo := &mockRouterRepository{
		updateLastSeenFn: func(_ context.Context, _ uuid.UUID) error {
			return errors.New("db error")
		},
	}
	svc := routerservice.New(testLogger(), repo)

	// When
	err := svc.UpdateLastSeen(context.Background(), routerID)

	// Then
	require.Error(t, err)
}

func TestUpdateLastSeen_WhenRouterNotFound_ReturnsError(t *testing.T) {
	// Given: репозиторий сообщает, что роутер не найден
	repo := &mockRouterRepository{
		updateLastSeenFn: func(_ context.Context, _ uuid.UUID) error {
			return errors.New("router not found")
		},
	}
	svc := routerservice.New(testLogger(), repo)

	// When
	err := svc.UpdateLastSeen(context.Background(), uuid.Nil)

	// Then
	require.Error(t, err)
}
