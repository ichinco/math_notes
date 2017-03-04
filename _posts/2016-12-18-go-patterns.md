---

layout: post
title: A Go Pattern For Better Unit Testing
tags: golang, go, code organisation

---

I want to reach a higher level of unit test coverage
in my code. In python, this is often quite doable without a lot
of efforts: during test time, you can set any method or function 
--- imported or built-in --- to be its mocked equivalent. The trick 
is then to keep the mocked methods organised, and there are a number of
packages that allow you to do this.

This is trickier in go. First, it's not possible to redefine methods
at will. For example, the following will not compile:

```go
type Foo struct { /* ... */ }

func (foo *Foo) Bar() { /* ... */ }

func main() {
        foo := Foo{}
        
        // cannot assign to foo.Bar
        foo.Bar = func() { /* ... */ }
        
        foo.Bar()
}
```

Furthermore, if your code is heavy on side effects --- such as modifying an external
data store by making library calls to a connector or a transaction --- then some care
has to be taken to expose the library functions or methods so that assertions can be
made about the API calls. Why you would or would not want to unit test certain aspects
of your code aside, it is definitely of some interest to think about ways that would
increase unit test coverage of your application.

We introduce this pattern in the context of the following common use cases: opening 
a file for reading. The idea here is to test as much of the code as possible. While 
in general this may be somewhat wasteful, it is of practical interest to

- achieve 100% unit test coverage of code
- test that the correct API calls are made to external libraries given proper inputs
- exercise the critical code paths and all error scenarios to ensure that they have
  been properly handled

Whatever the practical merits are for doing this, and notwithstanding the huge
effort that would pour into implementing the unit tests, knowing how to do it
would help you with more effective code design for unit level verifications.
To that end, here are some ideas.

### Initial Code

Let's open a file for reading some data. Here is one way one might do this in Go:

```go
package data

import (
        "io"
        "os"
        "io/ioutil"
        "github.com/golang/glog"
        "github.com/pkg/errors"
)

type Data struct { /* ... */ }
type DataMarshaler struct { /* ... */ }

func (marshaler *DataMarshaler) Marshal(reader io.Reader) (*Data, error) {
        data := Data{}
        /* Magic! */
        return &data, nil
}


func ReadData(filePath string) (*Data, error) {
        // open the file for reading
        dataFile, ioErr := os.Open(filePath)
        if ioErr != nil {
                glog.Errorf("Error opening file: %v", ioErr)
                return nil, errors.Wrap(ioErr, "Error opening file.")
        }
        defer dataFile.Close()
        
        marshaler := DataMarshaler { 
                /* ... */ 
        }
        
        // return the marshaler's output
        return marshaler.Marshal(dataFile)
}
```

To limit the scope, suppose we have very good test coverage for `marshaler`.
After all, the `struct` author thought ahead and used the `io.Reader` interface
as input, which allowed us to pass in a `bytes.Buffer` --- smart! But what about
`ReadData`?

One possible way to test this code is to actually use files: one for each positive test case that
you want to exercise; a few invalid file paths or improperly permissioned files to generate
common IO errors for the negative test cases. Another way tests is to stub out `os.Open`
and pass in a substitute function instead. While there are practical advantages to the former
approach, we will instead focus on the latter strategy. Some
advantages for the latter approach are:

- the test data files may be hard to generate (because they are large, those files may need 
  to be generated programmatically, or files are media or binary in nature)
- it is easier to manage test code that pretends to be files than to manage test data files
  (when your library changes, it is easier to change test code, and harder to change test 
  files)
- the test cases may be more visible and easier to comprehend, whereby a test writer
  would otherwise have to map file names to test cases, and test cases to file content
- the tests are conducive to benchmarking or stress testing which may be available only via
  code (TB of static data files or a streaming ByteBuffer that programmatically generates 
  data)
- conceptually the tests are better encapsulated in the sense that all test preambles live 
  entirely in the test code/binary itself; the correctness of tests is wholly determined 
  by code and code alone
  
Whatever the benefits may be, the `os.Open` is an archetypical example of any
library function whose (potentially complicated) behaviours are driven by
external states that can be mocked, and whose outputs are readily usable data structures
or interface objects. Other examples include `rand.Float32`, `time.Now`, or `s3.New`.

### Refactoring Pattern

To mock out `os.Open` in the example above, we want to perform the following refactoring:

- define (implicitly) `dataFile` to be an `io.ReadCloser`
- create a singleton object that tracks all external functions via private fields. 
  In particular, the `open` field --- a function of signature `func(string) (*os.File, error)`
  is assigned to `os.Open` during struct initialisation
- wrap the singleton object's `open` field in a function that returns `io.ReadCloser` instead
- instead of using `os.Open` directly in `ReadData`, use the wrapper function
  
In code this looks like:

```go
package data

import (
        "io"
        "os"
        "io/ioutil"
        "github.com/golang/glog"
        "github.com/pkg/errors"
)

/* as before */
type Data struct { /* ... */ }
type DataMarshaler struct { /* ... */ }
func (marshaler *DataMarshaler) Marshal(reader io.Reader) (*Data, error) { 
        data := Data{}
        /* ... */ 
        return &data, nil
}

// A singleton object that tracks all external functions
var __external__ *external_functions
type external_functions struct {
        open func(string) (*os.File, error)
        // etc
}

func EXTERNAL() *external_functions {
        if __external__ == nil {
                __external__ = &external_functions {
                        open: os.Open,
                        // etc
                }
        } 
        
        return __external__
}

// Wrapping __external__.open() in a slightly different return type:
// (io.ReadCloser, error) so that we are dealing completely with
// interface objects.
func open(filePath string) (io.ReadCloser, error) {
        external := EXTERNAL()
        return external.open(filePath)
}

func ReadData(filePath string) (*Data, error) {
        // open the file for reading
        dataFile, ioErr := open(filePath)
        if ioErr != nil {
                glog.Errorf("Error opening file: %v", ioErr)
                return nil, errors.Wrap(ioErr, "Error opening file.")
        }
        defer dataFile.Close()
        
        marshaler := DataMarshaler { 
                /* ... */ 
        }
        
        // return the marshaler's output
        return marshaler.Marshal(dataFile)
}
```

Now we can test away:

```go
package data

import (
        "testing"
        "io"
        "github.com/pkg/errors"
)
type MockOpen struct {
        Content []byte
        Error error
        
        filePath string
}

func NewMockOpen(content []byte, err error) *MockOpen {
        return &MockOpen {
                Content: content,        
                Error: error,
        }
}

func (mock *MockOpen) Open(filePath string) (io.Reader, error) {
        mock.filePath = filePath 
        return mock.Content, mock.Error
}

/* Test postiive */
func TestReadData(t *testing.T) {
        testCases := []struct {
                FileName string
                Content []byte
                Data *Data
        }{
                { /* ... */ },
        }
        
        external := EXTERNAL()
        oldOpen := external.open
        defer func() { external.open = oldOpen }()
        
        for i, c := range testCases {
                mock := NewMockOpen(c.Content, nil)
                external.open = mock.Open
                
                // verify that no error occurred in the process
                data, err := ReadData(c.FileName)
                if err != nil {
                        t.Error(/* ... */)
                }
                
                // verify that the correct parametre(s) is (are) 
                // passed to the external function
                if mock.filePath != c.FileName {
                        t.Error(/* ... */)
                }
        }
}

func TestReadDataError(t *testing.T) {
        external := EXTERNAL()
        oldOpen := external.open
        defer func() { external.open = oldOpen }()
        
        mock := NewMockOpen([]byte{}, errors.New("File open error."))
        external.open = mock.Open
        
        _, err := ReadData("random")
        
        // check that the function handles error correctly
        if err == nil {
                t.Error(/* ... */)
        }
        
}
```

We may also want to verify that the external methods are correctly assigned
in the singleton object:

```go
import (
        "reflect"
        "testing"
)
    
type TestNewExternal(t *testing.T) {
        external := NewExternal() 
        
        openFn := reflect.ValueOf(external.open)
        expectedOpenFn := reflect.ValueOf(os.Open)
        
        if openFn.Pointer() != expectedOpenFn.Pointer() {
                t.Error(/* ... */)
        }
        
        // etc
}
```

Now we're all covered! All for the price of a little (arguably necessary) 
increase in code complexity. What you have gained with this code are:

- programmatic access to the content of the stubbed input data
- 100% test coverage (for what it's worth)
- ease of generating new test cases and more logically complete input coverage
- semantically apparent test cases in the test source file itself

However, it is worth noting that there are some trade-offs. Here are a few that
come to mind:

- there is at least one layer of redirection: rather than making an API call
  directly, we are wrapping the library function in potentially multiple layers,
  and this pattern can be confusing to someone new to the code-base
- there are some performance penalties paid by creating objects of function
  pointers. In isolation, the penalities are relatively scant, but in aggregate,
  the penalities add up. If performance over testability is a big factor, don't
  do this.
- factoring out external library function is particularly time consuming, and
  greatly impacts velocity of development in two ways. It takes some time to
  plan out, and it adds some complexity to make changes. These are also 
  considerations for having more of your code nailed down by unit tests early
  in the application dev cycle. I would lean more towards this level of 
  refactoring for more mature library code. However, when seasoned code needs this
  level of refactoring, refactoring may be risky, especially since one of the reasons for 
  this type of refactoring was a dearth of test coverage. There is a very valid
  question of when is an optimal time for doing this type of refactoring.
