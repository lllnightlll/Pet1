/*
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <semaphore.h>
#include <unistd.h>

#define N 8
#define NR 3
#define NW 2

int db[N];
int nreaders;
sem_t semW;
sem_t semRcnt;
sem_t semNoReaders;

void *writer(void *arg)
{
    int id;
    int i;
    int j;

    id = *(int *)arg;
    i = 0;
    while (i < 6) {
        sem_wait(&semNoReaders);
        sem_wait(&semW);

        printf("W%d START tx\n", id);
        fflush(stdout);
        j = 0;
        while (j < N) {
            db[j] = id * 100 + i;
            delay(80);
            j = j + 1;
        }
        printf("W%d COMMIT %d\n", id, id * 100 + i);
        fflush(stdout);

        sem_post(&semW);
        sem_post(&semNoReaders);
        delay(200);
        i = i + 1;
    }
    return 0;
}

void *reader(void *arg)
{
    int id;
    int i;
    int j;

    id = *(int *)arg;
    i = 0;
    while (i < 12) {
        sem_wait(&semRcnt);
        nreaders = nreaders + 1;
        if (nreaders == 1)
            sem_wait(&semNoReaders);
        sem_post(&semRcnt);

        printf("R%d:", id);
        j = 0;
        while (j < N) {
            printf(" %d", db[j]);
            j = j + 1;
        }
        printf("\n");
        fflush(stdout);

        sem_wait(&semRcnt);
        nreaders = nreaders - 1;
        if (nreaders == 0)
            sem_post(&semNoReaders);
        sem_post(&semRcnt);

        delay(120);
        i = i + 1;
    }
    return 0;
}

int main(void)
{
    pthread_t th[NR + NW];
    int ids[NR + NW];
    int i;

    nreaders = 0;
    i = 0;
    while (i < N) {
        db[i] = 0;
        i = i + 1;
    }

    sem_init(&semW, 0, 1);
    sem_init(&semRcnt, 0, 1);
    sem_init(&semNoReaders, 0, 1);

    i = 0;
    while (i < NR) {
        ids[i] = i;
        pthread_create(&th[i], NULL, reader, &ids[i]);
        i = i + 1;
    }
    i = 0;
    while (i < NW) {
        ids[NR + i] = i;
        pthread_create(&th[NR + i], NULL, writer, &ids[NR + i]);
        i = i + 1;
    }
    i = 0;
    while (i < NR + NW) {
        pthread_join(th[i], NULL);
        i = i + 1;
    }
    return 0;
}
*/