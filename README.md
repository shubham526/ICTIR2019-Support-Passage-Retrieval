# Support Passage Retrieval
## Installaing the code
Download the install script from the online appendix and run it. If you get a permission denied error, do `chmod +x install.sh` and then try again.

## Running the code
On successful completion of the install script, the code is installed as an executable Java JAR file. This JAR file can be found in the folder called `target`. The JAR file is called `support-passage-1.0-SNAPSHOT-jar-with-dependencies.jar`. 

The JAR can be run from the command line with a variety of options, each of which creates a support passage run using one of the methods described in the paper.

### Available Options
The following options are available when running the code.
- baseline1 : Produces the first baseline
- baseline2 : Produces the second baseline
- ecn       : Produces a run using method "Entity Context Neighbors"
- pdrs      : Produces a run using method "Retrieval Score of ECD"
