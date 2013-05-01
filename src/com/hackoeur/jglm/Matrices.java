/* Copyright (C) 2013 James L. Royalty
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hackoeur.jglm;

import com.hackoeur.jglm.support.FastMath;

/**
 * Utility methods that replace OpenGL and GLU matrix functions there were 
 * deprecated in GL 3.0.
 * 
 * @author James Royalty
 */
public final class Matrices {
    
    
	public static final Mat4 translate
	(
		final Mat4 m,
		final Vec3 v
	)
	{
		//Result[3] = m[0] * v[0] + m[1] * v[1] + m[2] * v[2] + m[3];
                Vec4 col3 = m.<Vec4>getColumn(0).scale(v.x).add(
                        m.<Vec4>getColumn(1).scale(v.y)).add(
                        m.<Vec4>getColumn(2).scale(v.z)).add(
                        m.<Vec4>getColumn(3));
                Mat4 Result = new Mat4(
                        m.<Vec4>getColumn(0),
                        m.<Vec4>getColumn(1),
                        m.<Vec4>getColumn(2),col3);
		return Result;
	}
        
        public static final Mat4 rotate
	(
		final Mat4 m,
		final float angle, 
		final Vec3 v
	)
	{
		float a = (float)FastMath.toRadians(angle);
                
		float c = (float)FastMath.cos(a);
		float s = (float)FastMath.sin(a);

		Vec3 axis = v.getUnitVector();

		Vec3 temp = axis.scale(1f - c);

		float
                        rot00 = c + temp.x * axis.x,
                        rot01 = 0 + temp.x * axis.y + s * axis.z,
                        rot02 = 0 + temp.x * axis.z - s * axis.y,

                        rot10 = 0 + temp.y * axis.x - s * axis.z,
                        rot11 = c + temp.y * axis.y,
                        rot12 = 0 + temp.y * axis.z + s * axis.x,

                        rot20 = 0 + temp.z * axis.x + s * axis.y,
                        rot21 = 0 + temp.z * axis.y - s * axis.x,
                        rot22 = c + temp.z * axis.z;

                Mat4 Result = new Mat4(
                    m.<Vec4>getColumn(0) .scale( rot00 ) .add( m.<Vec4>getColumn(1) .scale( rot01 ) ) .add( m.<Vec4>getColumn(2) .scale( rot02 ) ),
                    m.<Vec4>getColumn(0) .scale( rot10 ) .add( m.<Vec4>getColumn(1) .scale( rot11 ) ) .add( m.<Vec4>getColumn(2) .scale( rot12 ) ),
                    m.<Vec4>getColumn(0) .scale( rot20 ) .add( m.<Vec4>getColumn(1) .scale( rot21 ) ) .add( m.<Vec4>getColumn(2) .scale( rot22 ) ),
                    m.<Vec4>getColumn(3)
                );
		return Result;
	}
        
        

	public static final Mat4 scale
	(
		final Mat4 m,
		final Vec3 v
	)
	{
		Mat4 Result = new Mat4(
                    m.<Vec4>getColumn(0).scale(v.x),
                    m.<Vec4>getColumn(1).scale(v.y),
                    m.<Vec4>getColumn(2).scale(v.z),
                    m.<Vec4>getColumn(3)
                );
		return Result;
	}
    
	/**
	 * Creates a perspective projection matrix using field-of-view and 
	 * aspect ratio to determine the left, right, top, bottom planes.  This
	 * method is analogous to the now deprecated {@code gluPerspective} method.
	 * 
	 * @param fovy field of view angle, in degrees, in the {@code y} direction
	 * @param aspect aspect ratio that determines the field of view in the x 
	 * direction.  The aspect ratio is the ratio of {@code x} (width) to 
	 * {@code y} (height).
	 * @param zNear near plane distance from the viewer to the near clipping plane (always positive)
	 * @param zFar far plane distance from the viewer to the far clipping plane (always positive)
	 * @return
	 */
	public static final Mat4 perspective(final float fovy, final float aspect, final float zNear, final float zFar) {
		final float halfFovyRadians = (float) FastMath.toRadians( (fovy / 2.0f) );
		final float range = (float) FastMath.tan(halfFovyRadians) * zNear;
		final float left = -range * aspect;
		final float right = range * aspect;
		final float bottom = -range;
		final float top = range;
		
		return new Mat4(
				(2f * zNear) / (right - left), 0f, 0f, 0f,
				0f, (2f * zNear) / (top - bottom), 0f, 0f,
				0f, 0f, -(zFar + zNear) / (zFar - zNear), -1f,
				0f, 0f, -(2f * zFar * zNear) / (zFar - zNear), 0f
		);
	}
	
	/**
	 * Creates a perspective projection matrix (frustum) using explicit
	 * values for all clipping planes.  This method is analogous to the now
	 * deprecated {@code glFrustum} method.
	 * 
	 * @param left left vertical clipping plane
	 * @param right right vertical clipping plane
	 * @param bottom bottom horizontal clipping plane
	 * @param top top horizontal clipping plane
	 * @param nearVal distance to the near depth clipping plane (must be positive)
	 * @param farVal distance to the far depth clipping plane (must be positive)
	 * @return
	 */
	public static final Mat4 frustum(final float left, final float right, final float bottom, final float top, final float nearVal, final float farVal) {
		final float m00 = (2f * nearVal) / (right - left);
		final float m11 = (2f * nearVal) / (top - bottom);
		final float m20 = (right + left) / (right - left);
		final float m21 = (top + bottom) / (top - bottom);
		final float m22 = -(farVal + nearVal) / (farVal - nearVal);
		final float m23 = -1f;
		final float m32 = -(2f * farVal * nearVal) / (farVal - nearVal);
		
		return new Mat4(
				m00, 0f, 0f, 0f, 
				0f, m11, 0f, 0f, 
				m20, m21, m22, m23, 
				0f, 0f, m32, 0f
		);
	}
	
	/**
	 * Defines a viewing transformation.  This method is analogous to the now
	 * deprecated {@code gluLookAt} method.
	 * 
	 * @param eye position of the eye point
	 * @param center position of the reference point
	 * @param up direction of the up vector
	 * @return
	 */
	public static final Mat4 lookAt(final Vec3 eye, final Vec3 center, final Vec3 up) {
		final Vec3 f = center.subtract(eye).getUnitVector();
		Vec3 u = up.getUnitVector();
		final Vec3 s = f.cross(u).getUnitVector();
		u = s.cross(f);
		
		return new Mat4(
				s.x, u.x, -f.x, 0f,
				s.y, u.y, -f.y, 0f,
				s.z, u.z, -f.z, 0f,
				-s.dot(eye), -u.dot(eye), f.dot(eye), 1f
		);
	}
	
	/**
	 * Creates an orthographic projection matrix.  This method is analogous to the now
	 * deprecated {@code glOrtho} method.
	 * 
	 * @param left left vertical clipping plane
	 * @param right right vertical clipping plane
	 * @param bottom bottom horizontal clipping plane
	 * @param top top horizontal clipping plane
	 * @param zNear distance to nearer depth clipping plane (negative if the plane is to be behind the viewer)
	 * @param zFar distance to farther depth clipping plane (negative if the plane is to be behind the viewer)
	 * @return
	 */
	public static final Mat4 ortho(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) {
		final float m00 = 2f / (right - left);
		final float m11 = 2f / (top - bottom);
		final float m22 = -2f / (zFar - zNear);
		final float m30 = - (right + left) / (right - left);
		final float m31 = - (top + bottom) / (top - bottom);
		final float m32 = - (zFar + zNear) / (zFar - zNear);
		
		return new Mat4(
				m00, 0f, 0f, 0f, 
				0f, m11, 0f, 0f,
				0f, 0f, m22, 0f, 
				m30, m31, m32, 1f
		);
	}
	
	/**
	 * Creates a 2D orthographic projection matrix.  This method is analogous to the now
	 * deprecated {@code gluOrtho2D} method.
	 * 
	 * @param left left vertical clipping plane
	 * @param right right vertical clipping plane
	 * @param bottom bottom horizontal clipping plane
	 * @param top top horizontal clipping plane
	 * @return
	 */
	public static final Mat4 ortho2d(final float left, final float right, final float bottom, final float top) {
		final float m00 = 2f / (right - left);
		final float m11 = 2f / (top - bottom);
		final float m22 = -1f;
		final float m30 = - (right + left) / (right - left);
		final float m31 = - (top + bottom) / (top - bottom);
		
		return new Mat4(
				m00, 0f, 0f, 0f, 
				0f, m11, 0f, 0f, 
				0f, 0f, m22, 0f, 
				m30, m31, 0f, 1f
		);
	}
}
