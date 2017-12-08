FROM lewg/mallet

RUN sed -i 's/^MEMORY=.*$/MEMORY=27g/' bin/mallet

ADD setup-memory.sh /
ADD import-directory.sh /
ADD infer-topics.sh /

RUN chmod u+x /setup-memory.sh /import-directory.sh /infer-topics.sh

CMD /setup-memory.sh; /import-directory.sh; /infer-topics.sh
