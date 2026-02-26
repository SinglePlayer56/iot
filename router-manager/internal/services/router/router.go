package router

import (
	"context"
	"errors"
	"log/slog"
	"router-manager/internal/domain/models"

	"github.com/google/uuid"
)

type RouterService struct {
	log              *slog.Logger
	routerRepository RouterRepository
}

type RouterRepository interface {
	GetAllRoutersId(ctx context.Context) ([]uuid.UUID, error)
	FindById(ctx context.Context, id uuid.UUID) (*models.Router, error)
	UpdateLastSeen(ctx context.Context, id uuid.UUID) error
}

func New(log *slog.Logger, routerRepository RouterRepository) *RouterService {
	return &RouterService{
		log:              log,
		routerRepository: routerRepository,
	}
}

func (s *RouterService) GetAllRoutersId(ctx context.Context) ([]uuid.UUID, error) {
	if s.routerRepository == nil {
		return nil, errors.New("router repository is nil")
	}

	return s.routerRepository.GetAllRoutersId(ctx)
}

func (s *RouterService) FindById(ctx context.Context, id uuid.UUID) (*models.Router, error) {
	if s.routerRepository == nil {
		return nil, errors.New("router repository is nil")
	}

	return s.routerRepository.FindById(ctx, id)
}

func (s *RouterService) UpdateLastSeen(ctx context.Context, id uuid.UUID) error {
	if s.routerRepository == nil {
		return errors.New("router repository is nil")
	}
	return s.routerRepository.UpdateLastSeen(ctx, id)
}
