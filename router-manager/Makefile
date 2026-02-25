PROTO_SRC_DIR := proto
PROTO_OUT_DIR := internal/gen/proto
PROTO_FILES := $(PROTO_SRC_DIR)/api/v1/send-command.proto

proto-generate:
	protoc -I $(PROTO_SRC_DIR) \
		--go_out=$(PROTO_OUT_DIR) --go_opt=paths=source_relative \
		--go-grpc_out=$(PROTO_OUT_DIR) --go-grpc_opt=paths=source_relative \
		$(PROTO_FILES)

