package grpc

import (
	"context"
	"encoding/json"
	"errors"
	"router-manager/internal/domain/models"
	apiv1 "router-manager/internal/gen/proto/api/v1"
	"time"

	"github.com/avito-tech/go-transaction-manager/trm/v2"

	commandservice "router-manager/internal/services/command"
	routerservice "router-manager/internal/services/router"

	"github.com/google/uuid"
	"google.golang.org/grpc"
	"google.golang.org/protobuf/encoding/protojson"
	"google.golang.org/protobuf/types/known/emptypb"
	"google.golang.org/protobuf/types/known/structpb"
)

type serverApi struct {
	apiv1.UnimplementedRouterManagerServer
	txManager      trm.Manager
	commandService *commandservice.CommandService
	routerService  *routerservice.RouterService
}

func Register(
	gRPC *grpc.Server,
	txManager trm.Manager,
	commandService *commandservice.CommandService,
	routerService *routerservice.RouterService,
) {
	apiv1.RegisterRouterManagerServer(
		gRPC,
		&serverApi{
			txManager:      txManager,
			commandService: commandService,
			routerService:  routerService,
		},
	)
}

func (s *serverApi) SendCommand(
	ctx context.Context,
	req *apiv1.SendCommandRequest,
) (*emptypb.Empty, error) {
	payload, err := protojson.Marshal(req.Payload)
	if err != nil {
		return nil, err
	}

	commandType := models.CommandType(req.GetCommandType())
	createdAt := time.Now()

	if req.GetRouterId() != "" {
		routerID, err := parseUUID(req.GetRouterId())
		if err != nil {
			return nil, err
		}

		command := newPendingCommand(routerID, commandType, payload, createdAt)
		_, err = s.commandService.SaveCommand(ctx, command)
		if err != nil {
			return nil, err
		}
	} else {
		err = s.withTx(ctx, func(txCtx context.Context) error {
			routerIDs, err := s.routerService.GetAllRoutersId(txCtx)
			if err != nil {
				return err
			}

			commands := buildPendingCommands(routerIDs, commandType, payload, createdAt)
			return s.commandService.SaveCommandsBatch(txCtx, commands)
		})
		if err != nil {
			return nil, err
		}
	}

	return &emptypb.Empty{}, nil
}

func (s *serverApi) Poll(
	ctx context.Context,
	req *apiv1.PollRequest,
) (*apiv1.PollResponse, error) {
	routerID, err := parseUUID(req.GetRouterId())
	if err != nil {
		return nil, err
	}

	var protoCommands []*apiv1.Command
	err = s.withTx(ctx, func(txCtx context.Context) error {
		if err := s.routerService.UpdateLastSeen(txCtx, routerID); err != nil {
			return err
		}

		commands, err := s.commandService.GetCommands(txCtx, routerID)
		if err != nil {
			return err
		}

		commandIDs := collectCommandIDs(commands)
		if err := s.commandService.SetSendCommandsBatch(txCtx, routerID, commandIDs); err != nil {
			return err
		}

		protoCommands = make([]*apiv1.Command, 0, len(commands))
		for _, command := range commands {
			protoCommand, err := toProtoCommand(command)
			if err != nil {
				return err
			}
			protoCommands = append(protoCommands, protoCommand)
		}

		return nil
	})
	if err != nil {
		return nil, err
	}

	return &apiv1.PollResponse{
		Commands: protoCommands,
	}, nil
}

func (s *serverApi) Ack(
	ctx context.Context,
	req *apiv1.AckRequest,
) (*emptypb.Empty, error) {
	commandID, err := parseUUID(req.GetCommandId())
	if err != nil {
		return nil, err
	}
	routerID, err := parseUUID(req.GetRouterId())
	if err != nil {
		return nil, err
	}

	err = s.withTx(ctx, func(txCtx context.Context) error {
		if err := s.routerService.UpdateLastSeen(txCtx, routerID); err != nil {
			return err
		}

		return s.commandService.SetAckCommand(txCtx, routerID, commandID)
	})
	if err != nil {
		return nil, err
	}

	return &emptypb.Empty{}, nil
}

func (s *serverApi) withTx(ctx context.Context, fn func(ctx context.Context) error) error {
	if s.txManager == nil {
		return errors.New("tx manager is nil")
	}

	return s.txManager.Do(ctx, fn)
}

func newPendingCommand(
	routerID uuid.UUID,
	commandType models.CommandType,
	payload []byte,
	createdAt time.Time,
) models.Command {
	return models.Command{
		ID:          uuid.New(),
		RouterID:    &routerID,
		CommandType: commandType,
		Payload:     json.RawMessage(payload),
		Status:      models.PENDING,
		SentAt:      nil,
		AckedAt:     nil,
		CreatedAt:   createdAt,
	}
}

func buildPendingCommands(
	routerIDs []uuid.UUID,
	commandType models.CommandType,
	payload []byte,
	createdAt time.Time,
) []models.Command {
	commands := make([]models.Command, 0, len(routerIDs))
	for _, routerID := range routerIDs {
		command := newPendingCommand(routerID, commandType, payload, createdAt)
		commands = append(commands, command)
	}

	return commands
}

func collectCommandIDs(commands []models.Command) []uuid.UUID {
	commandIDs := make([]uuid.UUID, 0, len(commands))
	for _, command := range commands {
		commandIDs = append(commandIDs, command.ID)
	}

	return commandIDs
}

func parseUUID(value string) (uuid.UUID, error) {
	return uuid.Parse(value)
}

func toProtoCommand(command models.Command) (*apiv1.Command, error) {
	payload, err := rawJSONToStruct(command.Payload)
	if err != nil {
		return nil, err
	}

	return &apiv1.Command{
		CommandId:   command.ID.String(),
		CommandType: string(command.CommandType),
		Payload:     payload,
	}, nil
}

func rawJSONToStruct(data json.RawMessage) (*structpb.Struct, error) {
	if len(data) == 0 {
		return nil, nil
	}

	var m map[string]any
	if err := json.Unmarshal(data, &m); err != nil {
		return nil, err
	}

	return structpb.NewStruct(m)
}
