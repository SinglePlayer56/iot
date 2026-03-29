package config

import (
	"flag"
	"fmt"
	"os"
	"time"

	"github.com/ilyakaznacheev/cleanenv"
)

type Config struct {
	GRPC          GRPCConfig     `yaml:"grpc"`
	Commands      CommandsConfig `yaml:"commands"`
	MigrationPath string         `yaml:"migrations_path"`
	Database      DatabaseConfig `yaml:"database"`
}

type DatabaseConfig struct {
	Url string `yaml:"url" env:"DATABASE_URL"`
}

type GRPCConfig struct {
	Port int `yaml:"port" env:"GRPC_PORT"`
}

type CommandsConfig struct {
	TTL time.Duration `yaml:"ttl" env:"COMMANDS_TTL"`
}

func MustLoad() *Config {
	configPath := fetchConfigPath()
	if configPath == "" {
		panic("config file path is empty")
	}

	return MustLoadPath(configPath)
}

func MustLoadPath(configPath string) *Config {
	if _, err := os.Stat(configPath); os.IsNotExist(err) {
		panic(fmt.Sprintf("config file not found: %s", configPath))
	}

	var config Config

	if err := cleanenv.ReadConfig(configPath, &config); err != nil {
		panic(fmt.Sprintf("cannot read config: %s", err.Error()))
	}

	return &config
}

func fetchConfigPath() string {
	var configPath string

	flag.StringVar(&configPath, "config", "config/config.yaml", "path to config file")
	flag.Parse()

	return configPath
}
