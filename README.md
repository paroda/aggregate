## Work in progress..

# aggregate
    
The purpose is to do bulk read/write of nested datastructures from several SQL databases with same schema. Load the data from one database and work on it like a project and then save it back to same database or another SQL database. Of course, the other database must have the same schema.
    
It should allow for matching rows between databases based on some unique constraint rather than just the ID column. This is because, the ID is auto-generated and can go out of sync between databases dependending on usage.
    
Currently testing with h2 and oracle database.
    
## Installation

Not yet in clojar.

## Usage

FIXME:


## License

Copyright © 2015 Pradyumna

Distributed under the Eclipse Public License version 1.0
