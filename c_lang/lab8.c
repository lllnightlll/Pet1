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
sem_t semW;

void *writer(void *arg)
{
    int id, i, j;
    id = *(int *)arg;
    for (i = 0; i < 6; i++) {
        sem_wait(&semW);
        printf("W%d START tx\n", id);
        fflush(stdout);
        for (j = 0; j < N; j++) {
            db[j] = id * 100 + i;
            delay(80);
        }
        printf("W%d COMMIT %d\n", id, id * 100 + i);
        fflush(stdout);
        sem_post(&semW);
        delay(200);
    }
    return 0;
}

void *reader(void *arg)
{
    int id, i, j;
    id = *(int *)arg;
    for (i = 0; i < 12; i++) {
        printf("R%d:", id);
        for (j = 0; j < N; j++)
            printf(" %d", db[j]);
        printf("\n");
        fflush(stdout);
        delay(120);
    }
    return 0;
}

int main(void)
{
    pthread_t th[NR + NW];
    int ids[NR + NW];
    int i;

    for (i = 0; i < N; i++)
        db[i] = 0;
    sem_init(&semW, 0, 1);

    for (i = 0; i < NR; i++) {
        ids[i] = i;
        pthread_create(&th[i], NULL, reader, &ids[i]);
    }
    for (i = 0; i < NW; i++) {
        ids[NR + i] = i;
        pthread_create(&th[NR + i], NULL, writer, &ids[NR + i]);
    }
    for (i = 0; i < NR + NW; i++)
        pthread_join(th[i], NULL);
    return 0;
}
    */