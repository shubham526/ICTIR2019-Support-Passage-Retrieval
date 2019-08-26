# Support Passage Retrieval
## Installing the code
Download the install script from the online appendix and run it. If you get a permission denied error, do `chmod +x install.sh` and then try again.

## Running the code
On successful completion of the install script, the code is installed as an executable Java JAR file. This JAR file can be found in the folder called `target`. The JAR file is called `support-passage-1.0-SNAPSHOT-jar-with-dependencies.jar`. 

The JAR can be run from the command line with a variety of options, each of which creates a support passage run using one of the methods described in the paper.

### Available Options
The following options are available when running the code.
- `baseline1` : Produces the first baseline
- `baseline2` : Produces the second baseline
- `ecn`       : Produces a run using method "Entity Context Neighbors"
- `pdrs`      : Produces a run using method "Retrieval Score of ECD"
- `qee`       : Produces a run using method "Query Expansion using Entities"
- `qew`       : Produces a run using method "Query Expansion using Words"
- `sal-exp-1` : Produces a run using the first salience experiment. (See paper)
- `sal-exp-2` : Produces a run using the second salience experiment. (See paper)

Each of the options above requires some command line arguments to work. Below, we describe the arguments required for each option.

### Command Line Arguments
We use the following vaiables to describe the command line arguments:

- `indexDir`          : Path to the Lucene index. 
- `supportPsgDir`     : Path to the support passage directory (where we store all the data, outputs, etc.)
- `outputDir`         : Path to the directory (within the support passage directory) where we want to store our outputs.
- `dataDir`           : Path to the directory (within the support passage directory) where we store all our data.
- `paraRunFile`       : Name of the candidate passage run file within the data directory.
- `entityRunFile`     : Name of the entity run file (provided) within the data directory.
- `outFile`           : Name of the output run file. (This will be stored in the directory passed using the `outputDir`.)
- `entityQrel`        : Path to the entity ground truth file. 
- `analyzer`          : Type of analyzer to use (`eng` for English and `std` for Standard). These refer to the Lucene analyzers.
- `similarity`        : Type of similarity to use (`bm25` for BM25, `lmds` for Language Model with Dirichlet Smoothing and `lmjm` for Language Model with Jelinek-Mercer Smoothing). These refer to the Lucene similarity. If choice is `lmjm`, you may have to specify an additional `lamda` value for the smoothing parameter.
- `omit`              : Whether to omit query terms during query expansion (RM1) or not (RM3). Can be either `yes` or `no`.
- `swatFile`          : Path to the serialized file containing the SWAT annotations. (Download from the online appendix).
- `supportPsgRunFile` : Path to the support passage run file
- `takeKEntities`     : Number of entities to use for query expansion.
- `takeKTerms`        : Number of terms to use for query expansion.
- `takeKDocs`         : Number of documents to use as feedback set for query expansion.

### Usage
- baseline1
```
java -jar target/support-passage-1.0-SNAPSHOT-jar-with-dependencies.jar baseline1 indexDir supportPsgDir outputDir dataDir paraRunFile entityRunFile outFile entityQrel 
```
- baseline2
```
java -jar target/support-passage-1.0-SNAPSHOT-jar-with-dependencies.jar baseline2 indexDir supportPsgDir outputDir dataDir entityRunFile entityQrel outFile analyzer similarity 
```
Note: If the choice for `similarity` is `lmjm`, this uses a default `lamda` value of 0.5. 

- ecn
```
java -jar target/support-passage-1.0-SNAPSHOT-jar-with-dependencies.jar ecn indexDir supportPsgDir outputDir dataDir paraRunFile entityRunFile outFile entityQrel 
```
- pdrs
```
java -jar target/support-passage-1.0-SNAPSHOT-jar-with-dependencies.jar pdrs indexDir supportPsgDir outputDir dataDir paraRunFile entityRunFile outFile entityQrel 
```
- qee
```
java -jar target/support-passage-1.0-SNAPSHOT-jar-with-dependencies.jar qee indexDir supportPsgDir outputDir dataDir paraRunFile entityRunFile entityQrel takeKEntities omit analyzer similarity [lambda]
```
Note: 

(1) If the choice for `similarity` is `lmjm`, then specify a `lamda` value. 

(2) The name of the output file is generated automatically from within the code. The format is: `qee_{similarity}_{rm1/rm3}`

- qew
```
java -jar target/support-passage-1.0-SNAPSHOT-jar-with-dependencies.jar qew indexDir supportPsgDir outputDir dataDir paraRunFile entityRunFile entityQrel takeKTerms takeKDocs omit analyzer similarity [lambda]
```
Note: 

(1) If the choice for `similarity` is `lmjm`, then specify a `lamda` value. 

(2) The name of the output file is generated automatically from within the code. The format is: `qew_{similarity}_{rm1/rm3}`
   
- sal-exp-1
```
java -jar target/support-passage-1.0-SNAPSHOT-jar-with-dependencies.jar sal-exp-1 indexDir supportPsgDir outputDir dataDir paraRunFile entityRunFile outFile entityQrel swatFile
```
- sal-exp-2
```
java -jar target/support-passage-1.0-SNAPSHOT-jar-with-dependencies.jar sal-exp-2 supportPsgDir outputDir dataDir supportPsgRunFile entityRunFile outFile swatFile
```
## Reproducabilty
We used the following values in our experiments:
- lambda = 0.4
- takeKEntities = 20
- takeKTerms = 50
- takeKDocs = 100

## Utility Scripts
Several utitlity scripts are provided to divide data into folds for k-fold cross-validation, create a RankLib compatible feature file, do the actual cross-validation, find Precision@1 and standard errors. See the repository [here](https://github.com/shubham526/scripts).
