package grpc

import (
	"context"
	apiv1 "router-manager/internal/gen/proto/api/v1"

	"google.golang.org/grpc"
	"google.golang.org/protobuf/types/known/emptypb"
)

type serverApi struct {
	apiv1.UnimplementedRouterManagerServer
}

func Register(gRPC *grpc.Server) {
	apiv1.RegisterRouterManagerServer(gRPC, &serverApi{})
}

func (s *serverApi) SendCommand(
	ctx context.Context,
	req *apiv1.SendCommandRequest,
) (*emptypb.Empty, error) {
	panic("implement me")
}

func (s *serverApi) Poll(
	ctx context.Context,
	req *apiv1.PollRequest,
) (*apiv1.PollResponse, error) {
	panic("implement me")
}

func (s *serverApi) Ack(
	ctx context.Context,
	req *apiv1.AckRequest,
) (*emptypb.Empty, error) {
	panic("implement me")
}
