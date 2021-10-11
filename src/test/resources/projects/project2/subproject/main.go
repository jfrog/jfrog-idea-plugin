package subproject

import (
	"fmt"

	"github.com/jfrog/gocmd/cmd"
)

func main() {
	cmd.GetDependenciesGraph()
}

func PrintHello() {
	fmt.Println("Hello World!")
}
