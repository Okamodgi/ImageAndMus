import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

public class Main {

    private static class DownloadTask {
        private final URL url;
        private final String destination;
        private final String name;

        public DownloadTask(URL url, String destination, String name) {
            this.url = url;
            this.destination = destination;
            this.name = name;
        }

        public void execute() throws IOException {
            try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                 FileOutputStream fos = new FileOutputStream(destination + name)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
        }
    }

    private static CompletableFuture<Void> downloadAndPlayMusic(URL url, String destination, String name) {
        return CompletableFuture.runAsync(() -> {
            try {
                DownloadTask musicTask = new DownloadTask(url, destination, name + ".mp3");
                musicTask.execute();
                playMusic(destination + name + ".mp3");
            } catch (IOException | JavaLayerException e) {
                throw new RuntimeException("Ошибка при скачивании и воспроизведении музыки", e);
            }
        });
    }

    private static CompletableFuture<Void> downloadImage(URL url, String destination, String name) {
        return CompletableFuture.runAsync(() -> {
            try {
                DownloadTask imageTask = new DownloadTask(url, destination, name + ".jpg");
                imageTask.execute();
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при скачивании изображения", e);
            }
        });
    }

    private static CompletableFuture<Void> readInputFromFile() {
        return CompletableFuture.runAsync(() -> {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader("src//file//inFile.txt"))) {
                String[] stringsImage = bufferedReader.readLine().split(" ");
                URL urlImage = new URL(stringsImage[0]);
                String strImage = stringsImage[1];

                String[] stringsMusic = bufferedReader.readLine().split(" ");
                URL urlMusic = new URL(stringsMusic[0]);
                String strMusic = stringsMusic[1];

                CompletableFuture<Void> imageTask = downloadImage(urlImage, strImage, "image");
                CompletableFuture<Void> musicTask = downloadAndPlayMusic(urlMusic, strMusic, "music");

                CompletableFuture.allOf(imageTask, musicTask).join();
            } catch (IOException e) {
                throw new RuntimeException("Ошибка чтения входных данных", e);
            }
        });
    }

    private static void playMusic(String filePath) throws IOException, JavaLayerException {
        System.out.println("Запущена песня");
        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            AdvancedPlayer player = new AdvancedPlayer(inputStream);
            player.setPlayBackListener(new PlaybackListener() {
                @Override
                public void playbackFinished(PlaybackEvent evt) {
                    if (evt.getType() == PlaybackEvent.STOPPED) {
                        player.close();
                    }
                }
            });
            player.play();
        }
    }

    public static void main(String[] args) {
        try {
            readInputFromFile().get(); // Ждем завершения всех задач
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Ошибка выполнения задач: " + e.getMessage());
        }
    }
}
