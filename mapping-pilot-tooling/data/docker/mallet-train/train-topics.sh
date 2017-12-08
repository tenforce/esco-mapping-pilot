#!/bin/bash

cd /usr/src/mallet
time bin/mallet train-topics  --input /data/proccessed/corpus_fr.mallet --num-topics $NUM_TOPICS --num-iterations $NUM_ITERATIONS --num-threads $NUM_THREADS --output-topic-keys /data/topics/fr_keys.txt --inferencer-filename /data/topics/fr_inferencer
