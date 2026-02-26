package grpc

import (
	"context"
	"encoding/json"
	"router-manager/internal/domain/models"
	apiv1 "router-manager/internal/gen/proto/api/v1"
	"time"

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
	commandService *commandservice.CommandService
	routerService  *routerservice.RouterService
}

func Register(
	gRPC *grpc.Server,
	commandService *commandservice.CommandService,
	routerService *routerservice.RouterService,
) {
	apiv1.RegisterRouterManagerServer(
		gRPC,
		&serverApi{
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
		routerIDs, err := s.routerService.GetAllRoutersId(ctx)
		if err != nil {
			return nil, err
		}

		commands := buildPendingCommands(routerIDs, commandType, payload, createdAt)

		err = s.commandService.SaveCommandsBatch(ctx, commands)
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

	if err := s.updateRouterLastSeen(ctx, routerID); err != nil {
		return nil, err
	}

	commands, err := s.commandService.GetCommands(ctx, routerID)
	if err != nil {
		return nil, err
	}

	commandIDs := collectCommandIDs(commands)
	if err := s.commandService.SetSendCommandsBatch(ctx, routerID, commandIDs); err != nil {
		return nil, err
	}

	protoCommands := make([]*apiv1.Command, 0, len(commands))
	for _, command := range commands {
		protoCommand, err := toProtoCommand(command)
		if err != nil {
			return nil, err
		}
		protoCommands = append(protoCommands, protoCommand)
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

	if err := s.updateRouterLastSeen(ctx, routerID); err != nil {
		return nil, err
	}

	err = s.commandService.SetAckCommand(ctx, routerID, commandID)
	if err != nil {
		return nil, err
	}

	return &emptypb.Empty{}, nil
}

func (s *serverApi) updateRouterLastSeen(ctx context.Context, routerID uuid.UUID) error {
	return s.routerService.UpdateLastSeen(ctx, routerID)
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
