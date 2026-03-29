package errors

import "fmt"

type InvalidArgumentError struct {
	Field   string
	Message string
}

func (e *InvalidArgumentError) Error() string {
	return fmt.Sprintf("Invalid argument %s: %s", e.Field, e.Message)
}

type NotFoundError struct {
	Entity string
	ID     string
}

func (e *NotFoundError) Error() string {
	return fmt.Sprintf("%s with id %s not found", e.Entity, e.ID)
}

type FailedPreconditionError struct {
	Message string
}

func (e *FailedPreconditionError) Error() string {
	return e.Message
}

type InternalError struct {
	Err error
}

func (e *InternalError) Error() string {
	return e.Err.Error()
}

func (e *InternalError) Unwrap() error {
	return e.Err
}
