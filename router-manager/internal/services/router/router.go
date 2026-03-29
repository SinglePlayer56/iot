package router

import (
	"context"
	"errors"
	"log/slog"

	"github.com/google/uuid"
)

type RouterService struct {
	log              *slog.Logger
	routerRepository RouterRepository
}

type RouterRepository interface {
	GetAllRoutersId(ctx context.Context) ([]uuid.UUID, error)
	UpdateLastSeen(ctx context.Context, id uuid.UUID) error
}

func New(log *slog.Logger, routerRepository RouterRepository) *RouterService {
	return &RouterService{
		log:              log,
		routerRepository: routerRepository,
	}
}

func (s *RouterService) GetAllRoutersId(ctx context.Context) ([]uuid.UUID, error) {
	s.log.Info("GetAllRoutersId")

	if s.routerRepository == nil {
		return nil, errors.New("router repository is nil")
	}

	ids, err := s.routerRepository.GetAllRoutersId(ctx)
	if err != nil {
		s.log.Error("GetAllRoutersId failed", slog.String("error", err.Error()))
		return nil, err
	}

	s.log.Info("GetAllRoutersId ok", slog.Int("count", len(ids)))
	return ids, nil
}

func (s *RouterService) UpdateLastSeen(ctx context.Context, id uuid.UUID) error {
	s.log.Info("UpdateLastSeen", slog.String("router_id", id.String()))

	if s.routerRepository == nil {
		return errors.New("router repository is nil")
	}

	if err := s.routerRepository.UpdateLastSeen(ctx, id); err != nil {
		s.log.Error("UpdateLastSeen failed", slog.String("router_id", id.String()), slog.String("error", err.Error()))
		return err
	}

	return nil
}
