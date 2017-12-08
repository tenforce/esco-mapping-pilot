#!/bin/bash

cd /usr/src/mallet
time bin/mallet import-dir --input /data/corpus/fr --output /data/proccessed/corpus_fr.mallet --keep-sequence --stoplist-file /data/stoplists/fr.txt