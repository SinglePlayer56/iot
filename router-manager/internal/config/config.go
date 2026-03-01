package config

import (
	"flag"
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
	Port    int           `yaml:"port"    env:"GRPC_PORT"`
	Timeout time.Duration `yaml:"timeout" env:"GRPC_TIMEOUT"`
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
		panic("config file not found: " + configPath)
	}

	var config Config

	if err := cleanenv.ReadConfig(configPath, &config); err != nil {
		panic("cannot read config: " + err.Error())
	}

	return &config
}

func fetchConfigPath() string {
	var configPath string

	flag.StringVar(&configPath, "config", "config/config.yaml", "path to config file")
	flag.Parse()

	return configPath
}
