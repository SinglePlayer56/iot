package models

import (
	"encoding/json"
	"time"

	"github.com/google/uuid"
)

type CommandStatus string

const (
	PENDING CommandStatus = "PENDING"
	SENT    CommandStatus = "SENT"
	ACKED   CommandStatus = "ACKED"
	FAILED  CommandStatus = "FAILED"
)

type CommandType string

const (
	REBOOT CommandType = "REBOOT"
	PING   CommandType = "PING"
)

type Command struct {
	ID          uuid.UUID       `db:"id"`
	RouterID    *uuid.UUID      `db:"router_id"`
	CommandType CommandType     `db:"command_type"`
	Payload     json.RawMessage `db:"payload"`
	Status      CommandStatus   `db:"status"`
	SentAt      *time.Time      `db:"sent_at"`
	AckedAt     *time.Time      `db:"acked_at"`
	CreatedAt   time.Time       `db:"created_at"`
}
