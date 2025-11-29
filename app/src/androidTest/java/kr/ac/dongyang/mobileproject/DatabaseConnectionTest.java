package kr.ac.dongyang.mobileproject;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class DatabaseConnectionTest {

    private static final String TAG = "DB_TEST";

    @Test
    public void testDatabaseConnection() throws InterruptedException {
        // 데이터베이스 연결은 비동기로 처리해야 하므로 CountDownLatch를 사용해 테스트 스레드가 대기하도록 함
        final CountDownLatch latch = new CountDownLatch(1);

        Executors.newSingleThreadExecutor().execute(() -> {
            Connection connection = null;
            Log.d(TAG, "Test Started");
            try {
                // DatabaseConnector를 통해 연결 시도
                connection = DatabaseConnector.getConnection();
                
                // 연결 객체가 null이 아니면 성공으로 간주
                assertNotNull("Database connection should not be null", connection);
                Log.d(TAG, "SUCCESS: Database connection successful.");

            } catch (Exception e) {
                // 예외 발생 시 테스트 실패 처리 및 로그 출력
                Log.e(TAG, "FAIL: Database connection failed.", e);
                fail("Database connection failed: " + e.getMessage());
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to close connection.", e);
                    }
                }
                // 비동기 작업이 끝났음을 알림
                latch.countDown();
            }
        });

        // 비동기 작업이 끝날 때까지 10초간 대기
        latch.await(10, TimeUnit.SECONDS);
    }
}
