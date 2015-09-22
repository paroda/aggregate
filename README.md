# aggregate
    
The purpose is to do bulk read/write of nested datastructures from several SQL databases with same schema. Load the data from one database and work on it like a project and then save it back to same database or another SQL database. Of course, the other database must have the same schema.
    
It should allow for matching rows between databases based on some unique constraint rather than just the ID column. This is because, the ID is auto-generated and can go out of sync between databases dependending on usage.

It should allow for renaming the columns to more convinient keywords using a simple schema. Often, the database column names are too long and with underscores. With renaming, it will be much convinient to use :active? in place of "IS_ACTIVE" or just :name in place of "EMPLOYEE_NAME".

Tested with h2 and oracle database.

At present, the it is not yet published to clojar. Till then, you can just download it and install to your local repo.

I am yet to provide the API documentation, which is not complete yet. For now, you can try go through the test files for example usage. Primarily this package provides the following public functions that you can use. For more please see the comments included in code.

- new-agg : to create a blank aggregate
- tempid : to create temporary id for specifying new entities in the aggregate
- unique-ids : to generate the ids based on specified unique fields. this serves as the basis to compare between databases
- build-er-config : to parse the specified entity relationship schema, and initialize this aggregate utility
- load-head-entity : to load entities including child entities
- load-family-heads : to load top level entities in the specified family, but without the child entities
- load-family : to load a group of entities part the specified family
- delete-entity! : to delete a entity including all child entities
- delete-agg! : to delete all entities specified in the given aggregate
- save-agg! : to save the given aggregate to a target database
- merge-agg : to merge an aggregate with another
- reduce-agg : to do an aggregate evaluation by applying a specified function to each entity



## License

Copyright © 2015 Pradyumna

Distributed under the Eclipse Public License version 1.0
