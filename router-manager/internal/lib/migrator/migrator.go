package migrator

import (
	"context"
	"errors"
	"fmt"
	"path/filepath"

	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
)

func Up(ctx context.Context, migrationsPath, databaseURL string) error {
	path, err := filepath.Abs(migrationsPath)
	if err != nil {
		return fmt.Errorf("resolve migrations path: %w", err)
	}

	m, err := migrate.New("file://"+path, databaseURL)
	if err != nil {
		return fmt.Errorf("init migrate: %w", err)
	}
	defer func() {
		_, _ = m.Close()
	}()

	/* Если во время выполнения миграций придет SIGINT/SIGTERM, тогда произойдёт отмена контекста,
	* тогда дождёмся окончания последней миграции и остановимся. */
	stopGracefulWatch := context.AfterFunc(ctx, func() {
		/* Неблокирующая отправка исключает редкое зависание,
		* если канал уже заполнен к моменту срабатывания callback. */
		select {
		case m.GracefulStop <- true:
		default:
		}
	})
	/* Если Up() завершится раньше отмены контекста, снимаем слушатель */
	defer stopGracefulWatch()

	if err := m.Up(); err != nil && !errors.Is(err, migrate.ErrNoChange) {
		return fmt.Errorf("apply migrations: %w", err)
	}

	/* Если контекст успели отменить во время выполнения Up(), считаем запуск прерванным.
	* В этот момент текущая миграция могла завершиться, но следующие уже не пойдут. */
	if err := ctx.Err(); err != nil {
		return fmt.Errorf("migrations interrupted: %w", err)
	}

	return nil
}
