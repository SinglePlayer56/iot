package commandexpiration

import (
	"context"
	"errors"
	"log/slog"
	"sync"
	"time"

	"github.com/avito-tech/go-transaction-manager/trm/v2"

	commandservice "router-manager/internal/services/command"
)

type App struct {
	log            *slog.Logger
	txManager      trm.Manager
	commandService *commandservice.CommandService
	ttl            time.Duration
	ctx            context.Context
	cancel         context.CancelFunc
	done           chan struct{}
	stopOnce       sync.Once
}

func New(
	parentCtx context.Context,
	log *slog.Logger,
	txManager trm.Manager,
	commandService *commandservice.CommandService,
	ttl time.Duration,
) *App {
	ctx, cancel := context.WithCancel(parentCtx)

	return &App{
		log:            log,
		txManager:      txManager,
		commandService: commandService,
		ttl:            ttl,
		ctx:            ctx,
		cancel:         cancel,
		done:           make(chan struct{}),
	}
}

func (a *App) MustRun() {
	if err := a.Run(); err != nil {
		panic(err)
	}
}

func (a *App) Run() error {
	if a.txManager == nil {
		return errors.New("transaction manager is nil")
	}
	if a.commandService == nil {
		return errors.New("command service is nil")
	}
	if a.ttl <= 0 {
		return errors.New("commands ttl must be positive")
	}

	if a.log != nil {
		a.log.Info(
			"command expiration worker started",
			slog.Duration("ttl", a.ttl),
		)
	}

	defer close(a.done)

	/* запускаем один раз при старте, чтобы пометить FAILED уже истёкшие команды */
	a.runOnce(a.ctx)

	ticker := time.NewTicker(a.ttl)
	defer ticker.Stop()

	for {
		select {
		case <-a.ctx.Done():
			if a.log != nil {
				a.log.Info("stopping command expiration worker")
			}
			return nil
		case <-ticker.C:
			a.runOnce(a.ctx)
		}
	}
}

func (a *App) Stop() {
	if a == nil {
		return
	}

	a.stopOnce.Do(func() {
		if a.cancel != nil {
			a.cancel()
		}
	})

	<-a.done
}

func (a *App) runOnce(ctx context.Context) {
	var expiredCount int

	err := a.txManager.Do(ctx, func(txCtx context.Context) error {
		count, err := a.commandService.ExpireOldCommands(txCtx, a.ttl)
		if err != nil {
			return err
		}
		expiredCount = count
		return nil
	})

	if err != nil {
		if a.log != nil && !errors.Is(err, context.Canceled) {
			a.log.Error("command expiration worker iteration failed", slog.String("error", err.Error()))
		}
		return
	}

	if expiredCount > 0 && a.log != nil {
		a.log.Info("expired commands marked as FAILED", slog.Int("count", expiredCount))
	}
}
