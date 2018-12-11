# motive-rdf

Motif induction for (RDF) knowledge graphs. 

## Installation

Clone or download the project and compile with maven. On the command line:
```
mvn clean install
```

To run one of the experiments, import it into an IDE like Eclipse and run ```Run.java```. Alternatively,
compile it into a jar using:
```
mvn clean install package -DskipTests
```

Then run one of the experiments by calling the jar For instance:
```
java -jar motive-rdf.jar --experiment real-world --iterations 100000 --topk 100 --data aifb --max-time 5
```

The datasets (mutag, aifb, dogfood) are packaged into the jar. Support for other data is coming soon. Send me a message or make an issue if you need this.

## Supplement

[Download the paper supplement](supplement.pdf), containing the extended results 
of the motif experiment.