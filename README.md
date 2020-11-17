# Lucene Assignment 1

# Indexing and mesuring the performence of the system using trecEval for cranfeild data set

## Build

`cd serch/pro/searchRepo`<br>
`mvn package`

## Run

`java -jar target/search_pro-1.0.jar`

## Evaulating the performance

```
cd trec_Eval
./trec_eval ../data/cranqrel ../../index/result
```
