#!/bin/bash

cd /usr/src/mallet
time bin/mallet infer-topics  --inferencer /data/topics/inferencer --input /data/proccessed/concepts.mallet --output-doc-topics /data/output.topics --doc-topics-threshold 0.0005
# time bin/mallet infer-topics  --inferencer /data/topics/fr_inferencer --input /data/proccessed/concepts_fr_skill.mallet --output-doc-topics /data/topics/pes-fr-skill.topics --doc-topics-threshold 0.0005
# time bin/mallet infer-topics  --inferencer /data/topics/fr_inferencer --input /data/proccessed/concepts_fr_esco_occ.mallet --output-doc-topics /data/topics/esco-fr-occ.topics --doc-topics-threshold 0.0005
# time bin/mallet infer-topics  --inferencer /data/topics/fr_inferencer --input /data/proccessed/concepts_fr_esco_skill.mallet --output-doc-topics /data/topics/esco-fr-skill.topics --doc-topics-threshold 0.0005
