package project1

import (
	"github.com/jfrog/jfrog-cli-core/artifactory/commands/curl"
	"github.com/jfrog/jfrog-cli-core/common/commands"
)

func main() {
	curl.NewRtCurlCommand(commands.CurlCommand{})
}
