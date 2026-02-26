package grpc

import (
	"fmt"
	"log/slog"
	"net"

	"github.com/avito-tech/go-transaction-manager/trm/v2"

	routermanagergrpc "router-manager/internal/grpc"
	commandservice "router-manager/internal/services/command"
	routerservice "router-manager/internal/services/router"

	"google.golang.org/grpc"
)

type App struct {
	log        *slog.Logger
	gRPCServer *grpc.Server
	port       int
}

func New(
	log *slog.Logger,
	port int,
	txManager trm.Manager,
	commandService *commandservice.CommandService,
	routerService *routerservice.RouterService,
) *App {
	grpcServer := grpc.NewServer()

	routermanagergrpc.Register(grpcServer, txManager, commandService, routerService)

	return &App{
		log:        log,
		gRPCServer: grpcServer,
		port:       port,
	}
}

func (a *App) MustRun() {
	if err := a.Run(); err != nil {
		panic(err)
	}
}

func (a *App) Run() error {

	l, err := net.Listen("tcp", fmt.Sprintf(":%d", a.port))
	if err != nil {
		return err
	}

	a.log.Info("gRPC server started", slog.String("addr", l.Addr().String()))

	if err := a.gRPCServer.Serve(l); err != nil {
		return err
	}

	return nil
}

func (a *App) Stop() {
	a.log.Info("stopping gRPC server", slog.Int("port", a.port))
	a.gRPCServer.GracefulStop()
}
