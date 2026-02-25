package models

import (
	"time"

	"github.com/google/uuid"
)

type Router struct {
	ID           uuid.UUID  `db:"id"`
	SerialNumber string     `db:"serial_number"`
	LastSeenAt   *time.Time `db:"last_seen_at"`
}
