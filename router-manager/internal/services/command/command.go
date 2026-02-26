package command

import (
	"context"
	"errors"
	"log/slog"
	"router-manager/internal/domain/models"

	"github.com/google/uuid"
)

type CommandService struct {
	log               *slog.Logger
	commandRepository CommandRepository
}

type CommandRepository interface {
	SaveCommand(ctx context.Context, command models.Command) (*models.Command, error)
	GetCommands(ctx context.Context, routerId uuid.UUID) ([]models.Command, error)
	SetSentCommandsBatch(ctx context.Context, routerId uuid.UUID, commandIds []uuid.UUID) error
	AckCommand(ctx context.Context, routerId uuid.UUID, commandId uuid.UUID) error
	SaveCommandsBatch(ctx context.Context, commands []models.Command) error
}

func New(logger *slog.Logger, commandRepository CommandRepository) *CommandService {
	return &CommandService{
		log:               logger,
		commandRepository: commandRepository,
	}
}

func (s *CommandService) SaveCommand(ctx context.Context, command models.Command) (*models.Command, error) {
	if s.commandRepository == nil {
		return nil, errors.New("command repository is nil")
	}

	if command.ID == uuid.Nil {
		command.ID = uuid.New()
	}

	if command.CommandType == "" {
		return nil, errors.New("command_type is required")
	}

	if command.Status == "" {
		command.Status = models.PENDING
	}

	return s.commandRepository.SaveCommand(ctx, command)
}

func (s *CommandService) GetCommands(ctx context.Context, routerId uuid.UUID) ([]models.Command, error) {
	if s.commandRepository == nil {
		return nil, errors.New("command repository is nil")
	}

	if routerId == uuid.Nil {
		return nil, errors.New("routerId is required")
	}

	return s.commandRepository.GetCommands(ctx, routerId)
}

func (s *CommandService) SetSendCommandsBatch(ctx context.Context, routerId uuid.UUID, commandIds []uuid.UUID) error {
	if s.commandRepository == nil {
		return errors.New("command repository is nil")
	}

	if routerId == uuid.Nil {
		return errors.New("routerId is required")
	}

	for _, commandId := range commandIds {
		if commandId == uuid.Nil {
			return errors.New("commandId is required")
		}
	}

	return s.commandRepository.SetSentCommandsBatch(ctx, routerId, commandIds)
}

func (s *CommandService) SetAckCommand(ctx context.Context, routerId uuid.UUID, commandId uuid.UUID) error {
	if s.commandRepository == nil {
		return errors.New("command repository is nil")
	}

	if routerId == uuid.Nil {
		return errors.New("routerId is required")
	}

	if commandId == uuid.Nil {
		return errors.New("commandId is required")
	}

	return s.commandRepository.AckCommand(ctx, routerId, commandId)
}

func (s *CommandService) SaveCommandsBatch(ctx context.Context, commands []models.Command) error {
	if s.commandRepository == nil {
		return errors.New("command repository is nil")
	}

	return s.commandRepository.SaveCommandsBatch(ctx, commands)
}
