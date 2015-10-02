/*
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */
package org.lwjgl.demo.opengl.geometry;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.libffi.Closure;
import org.joml.Matrix4f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.demo.opengl.util.DemoUtils.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.system.MemoryUtil.*;

public class GeometryShaderTest {

	long window;
	int width = 1024;
	int height = 768;

	int vao;
	int program;

	int viewMatrixUniform;
	int projMatrixUniform;
	int viewportSizeUniform;

	Matrix4f viewMatrix = new Matrix4f();
	Matrix4f projMatrix = new Matrix4f();
	ByteBuffer matrixByteBuffer = BufferUtils.createByteBuffer(4 * 16);

	GLCapabilities caps;
	GLFWErrorCallback errCallback;
	GLFWKeyCallback keyCallback;
	GLFWFramebufferSizeCallback fbCallback;
	Closure debugProc;

	void init() throws IOException {
		glfwSetErrorCallback(errCallback = new GLFWErrorCallback() {
			GLFWErrorCallback delegate = GLFWErrorCallback.createPrint(System.err);

			@Override
			public void invoke(int error, long description) {
				if (error == GLFW_VERSION_UNAVAILABLE)
					System.err.println("This demo requires OpenGL 3.2 or higher.");
				delegate.invoke(error, description);
			}

			@Override
			public void release() {
				delegate.release();
				super.release();
			}
		});

		if (glfwInit() != GL_TRUE)
			throw new IllegalStateException("Unable to initialize GLFW");

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
		glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);

		window = glfwCreateWindow(width, height, "Antialiased wireframe rendering with geometry shader", NULL, NULL);
		if (window == NULL) {
			throw new AssertionError("Failed to create the GLFW window");
		}

		glfwSetFramebufferSizeCallback(window, fbCallback = new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				if (width > 0 && height > 0
						&& (GeometryShaderTest.this.width != width || GeometryShaderTest.this.height != height)) {
					GeometryShaderTest.this.width = width;
					GeometryShaderTest.this.height = height;
				}
			}
		});

		glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				if (action != GLFW_RELEASE)
					return;

				if (key == GLFW_KEY_ESCAPE) {
					glfwSetWindowShouldClose(window, GL_TRUE);
				}
			}
		});

		GLFWvidmode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		glfwSetWindowPos(window, (vidmode.getWidth() - width) / 2, (vidmode.getHeight() - height) / 2);
		glfwMakeContextCurrent(window);
		glfwSwapInterval(0);
		glfwShowWindow(window);
		caps = GL.createCapabilities();
		debugProc = GLUtil.setupDebugMessageCallback();

		glClearColor(0.55f, 0.75f, 0.95f, 1.0f);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		/* Create all needed GL resources */
		createVao();
		createRasterProgram();
		initProgram();
	}

	void createVao() {
		this.vao = glGenVertexArrays();
		glBindVertexArray(vao);
		FloatBuffer pb = BufferUtils.createFloatBuffer(3 * 4 * 6);
		pb.put(0.5f).put(-0.5f).put(-0.5f);
		pb.put(-0.5f).put(-0.5f).put(-0.5f);
		pb.put(-0.5f).put(0.5f).put(-0.5f);
		pb.put(0.5f).put(0.5f).put(-0.5f);
		pb.put(0.5f).put(-0.5f).put(0.5f);
		pb.put(0.5f).put(0.5f).put(0.5f);
		pb.put(-0.5f).put(0.5f).put(0.5f);
		pb.put(-0.5f).put(-0.5f).put(0.5f);
		pb.put(0.5f).put(-0.5f).put(-0.5f);
		pb.put(0.5f).put(0.5f).put(-0.5f);
		pb.put(0.5f).put(0.5f).put(0.5f);
		pb.put(0.5f).put(-0.5f).put(0.5f);
		pb.put(-0.5f).put(-0.5f).put(0.5f);
		pb.put(-0.5f).put(0.5f).put(0.5f);
		pb.put(-0.5f).put(0.5f).put(-0.5f);
		pb.put(-0.5f).put(-0.5f).put(-0.5f);
		pb.put(0.5f).put(0.5f).put(0.5f);
		pb.put(0.5f).put(0.5f).put(-0.5f);
		pb.put(-0.5f).put(0.5f).put(-0.5f);
		pb.put(-0.5f).put(0.5f).put(0.5f);
		pb.put(0.5f).put(-0.5f).put(-0.5f);
		pb.put(0.5f).put(-0.5f).put(0.5f);
		pb.put(-0.5f).put(-0.5f).put(0.5f);
		pb.put(-0.5f).put(-0.5f).put(-0.5f);
		pb.flip();
		// build element buffer
		IntBuffer eb = BufferUtils.createIntBuffer(6 * 6);
		for (int i = 0; i < 4 * 6; i += 4)
			eb.put(i).put(i + 1).put(i + 2).put(i + 2).put(i + 3).put(i + 0);
		eb.flip();
		// setup vertex positions buffer
		int cubeVbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, cubeVbo);
		glBufferData(GL_ARRAY_BUFFER, pb, GL_STATIC_DRAW);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0L);
		// setup element buffer
		int cubeEbo = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cubeEbo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, eb, GL_STATIC_DRAW);
		glBindVertexArray(0);
	}

	void createRasterProgram() throws IOException {
		int program = glCreateProgram();
		int vshader = createShader("org/lwjgl/demo/opengl/geometry/vs.glsl", GL_VERTEX_SHADER);
		int fshader = createShader("org/lwjgl/demo/opengl/geometry/fs.glsl", GL_FRAGMENT_SHADER);
		int gshader = createShader("org/lwjgl/demo/opengl/geometry/gs.glsl", GL_GEOMETRY_SHADER);
		glAttachShader(program, vshader);
		glAttachShader(program, fshader);
		glAttachShader(program, gshader);
		glBindAttribLocation(program, 0, "position");
		glBindFragDataLocation(program, 0, "color");
		glLinkProgram(program);
		int linked = glGetProgrami(program, GL_LINK_STATUS);
		String programLog = glGetProgramInfoLog(program);
		if (programLog.trim().length() > 0) {
			System.err.println(programLog);
		}
		if (linked == 0) {
			throw new AssertionError("Could not link program");
		}
		this.program = program;
	}

	/**
	 * Initialize the shader program.
	 */
	void initProgram() {
		glUseProgram(this.program);
		viewMatrixUniform = glGetUniformLocation(this.program, "viewMatrix");
		projMatrixUniform = glGetUniformLocation(this.program, "projMatrix");
		viewportSizeUniform = glGetUniformLocation(this.program, "viewportSize");
		glUseProgram(0);
	}

	float angle = 0.0f;
	long lastTime = System.nanoTime();
	void update() {
		projMatrix.setPerspective((float) Math.toRadians(30), (float) width / height, 0.01f, 50.0f);
		long thisTime = System.nanoTime();
		float diff = (thisTime - lastTime) / 1E9f;
		angle += diff;
		viewMatrix.setLookAt(0.0f, 2.0f, 5.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f).rotateY(angle);
		lastTime = thisTime;
	}

	void render() {
		glUseProgram(this.program);

		glUniformMatrix4fv(viewMatrixUniform, 1, false, viewMatrix.get(matrixByteBuffer));
		glUniformMatrix4fv(projMatrixUniform, 1, false, projMatrix.get(matrixByteBuffer));
		glUniform2f(viewportSizeUniform, width, height);

		glBindVertexArray(vao);
		glDrawElements(GL_TRIANGLES, 3 * 2 * 6, GL_UNSIGNED_INT, 0L);
		glBindVertexArray(0);

		glUseProgram(0);
	}

	void loop() {
		while (glfwWindowShouldClose(window) == GL_FALSE) {
			glfwPollEvents();
			glViewport(0, 0, width, height);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			update();
			render();

			glfwSwapBuffers(window);
		}
	}

	void run() {
		try {
			init();
			loop();

			if (debugProc != null) {
				debugProc.release();
			}

			errCallback.release();
			keyCallback.release();
			fbCallback.release();
			glfwDestroyWindow(window);
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			glfwTerminate();
		}
	}

	public static void main(String[] args) {
		new GeometryShaderTest().run();
	}

}