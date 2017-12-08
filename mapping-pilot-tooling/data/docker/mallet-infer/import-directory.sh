#!/bin/bash

cd /usr/src/mallet
time bin/mallet import-dir --input /data/concepts/ --output /data/proccessed/concepts.mallet --keep-sequence --stoplist-file /data/stoplist.txt --use-pipe-from /data/proccessed/corpus.mallet
# time bin/mallet import-dir --input /data/concepts/mallet_hosp-skills --output /data/proccessed/concepts_fr_skill.mallet --keep-sequence --stoplist-file /data/stoplists/fr.txt --use-pipe-from /data/proccessed/corpus_fr.mallet
# time bin/mallet import-dir --input /data/concepts/mallet_occ-fr_final --output /data/proccessed/concepts_fr_esco_occ.mallet --keep-sequence --stoplist-file /data/stoplists/fr.txt --use-pipe-from /data/proccessed/corpus_fr.mallet
# time bin/mallet import-dir --input /data/concepts/mallet_skill-fr_final --output /data/proccessed/concepts_fr_esco_skill.mallet --keep-sequence --stoplist-file /data/stoplists/fr.txt --use-pipe-from /data/proccessed/corpus_fr.mallet
