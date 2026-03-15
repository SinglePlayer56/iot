package command

import (
	"context"
	"errors"
	"log/slog"
	"router-manager/internal/domain/models"
	"time"

	domainerrors "router-manager/internal/domain/errors"

	"github.com/google/uuid"
)

type CommandService struct {
	log               *slog.Logger
	commandRepository CommandRepository
}

const defaultPollBatchSize = 100

type CommandRepository interface {
	SaveCommand(ctx context.Context, command models.Command) (*models.Command, error)
	GetCommands(ctx context.Context, routerId uuid.UUID, limit int) ([]models.Command, error)
	SetSentCommandsBatch(ctx context.Context, routerId uuid.UUID, commandIds []uuid.UUID) error
	AckCommand(ctx context.Context, routerId uuid.UUID, commandId uuid.UUID) error
	SaveCommandsBatch(ctx context.Context, commands []models.Command) error
	ExpireSentCommandsBefore(ctx context.Context, threshold time.Time) (int, error)
}

func New(logger *slog.Logger, commandRepository CommandRepository) *CommandService {
	return &CommandService{
		log:               logger,
		commandRepository: commandRepository,
	}
}

func (s *CommandService) SaveCommand(ctx context.Context, command models.Command) (*models.Command, error) {
	s.log.Info("SaveCommand", slog.String("command_type", string(command.CommandType)))

	if s.commandRepository == nil {
		return nil, errors.New("command repository is nil")
	}

	if command.ID == uuid.Nil {
		command.ID = uuid.New()
	}

	if command.CommandType == "" {
		return nil, &domainerrors.InvalidArgumentError{Field: "command_type", Message: "is required"}
	}

	if command.Status == "" {
		command.Status = models.PENDING
	}

	saved, err := s.commandRepository.SaveCommand(ctx, command)
	if err != nil {
		s.log.Error("SaveCommand failed", slog.String("error", err.Error()))
		return nil, err
	}

	s.log.Info("SaveCommand ok", slog.String("command_id", saved.ID.String()))
	return saved, nil
}

func (s *CommandService) GetCommands(ctx context.Context, routerId uuid.UUID) ([]models.Command, error) {
	s.log.Info("GetCommands", slog.String("router_id", routerId.String()))

	if s.commandRepository == nil {
		return nil, errors.New("command repository is nil")
	}

	if routerId == uuid.Nil {
		return nil, &domainerrors.InvalidArgumentError{Field: "router_id", Message: "is required"}
	}

	commands, err := s.commandRepository.GetCommands(ctx, routerId, defaultPollBatchSize)
	if err != nil {
		s.log.Error("GetCommands failed", slog.String("router_id", routerId.String()), slog.String("error", err.Error()))
		return nil, err
	}

	s.log.Info("GetCommands ok", slog.String("router_id", routerId.String()), slog.Int("count", len(commands)))
	return commands, nil
}

func (s *CommandService) SetSendCommandsBatch(ctx context.Context, routerId uuid.UUID, commandIds []uuid.UUID) error {
	s.log.Info("SetSendCommandsBatch", slog.String("router_id", routerId.String()), slog.Int("count", len(commandIds)))

	if s.commandRepository == nil {
		return errors.New("command repository is nil")
	}

	if routerId == uuid.Nil {
		return &domainerrors.InvalidArgumentError{Field: "router_id", Message: "is required"}
	}

	for _, commandId := range commandIds {
		if commandId == uuid.Nil {
			return &domainerrors.InvalidArgumentError{Field: "command_id", Message: "is required"}
		}
	}

	if err := s.commandRepository.SetSentCommandsBatch(ctx, routerId, commandIds); err != nil {
		s.log.Error("SetSendCommandsBatch failed", slog.String("router_id", routerId.String()), slog.String("error", err.Error()))
		return err
	}

	return nil
}

func (s *CommandService) SetAckCommand(ctx context.Context, routerId uuid.UUID, commandId uuid.UUID) error {
	s.log.Info("SetAckCommand", slog.String("router_id", routerId.String()), slog.String("command_id", commandId.String()))

	if s.commandRepository == nil {
		return errors.New("command repository is nil")
	}

	if routerId == uuid.Nil {
		return &domainerrors.InvalidArgumentError{Field: "router_id", Message: "is required"}
	}

	if commandId == uuid.Nil {
		return &domainerrors.InvalidArgumentError{Field: "command_id", Message: "is required"}
	}

	if err := s.commandRepository.AckCommand(ctx, routerId, commandId); err != nil {
		s.log.Error("SetAckCommand failed", slog.String("router_id", routerId.String()), slog.String("command_id", commandId.String()), slog.String("error", err.Error()))
		return err
	}

	return nil
}

func (s *CommandService) SaveCommandsBatch(ctx context.Context, commands []models.Command) error {
	s.log.Info("SaveCommandsBatch", slog.Int("count", len(commands)))

	if s.commandRepository == nil {
		return errors.New("command repository is nil")
	}

	if err := s.commandRepository.SaveCommandsBatch(ctx, commands); err != nil {
		s.log.Error("SaveCommandsBatch failed", slog.String("error", err.Error()))
		return err
	}

	s.log.Info("SaveCommandsBatch ok", slog.Int("count", len(commands)))
	return nil
}

func (s *CommandService) ExpireOldCommands(ctx context.Context, ttl time.Duration) (int, error) {
	s.log.Info("ExpireOldCommands", slog.Duration("ttl", ttl))

	if s.commandRepository == nil {
		return 0, errors.New("command repository is nil")
	}

	if ttl <= 0 {
		return 0, &domainerrors.InvalidArgumentError{Field: "ttl", Message: "must be positive"}
	}

	threshold := time.Now().Add(-ttl)
	count, err := s.commandRepository.ExpireSentCommandsBefore(ctx, threshold)
	if err != nil {
		s.log.Error("ExpireOldCommands failed", slog.String("error", err.Error()))
		return 0, err
	}

	s.log.Info("ExpireOldCommands ok", slog.Int("expired", count))
	return count, nil
}
